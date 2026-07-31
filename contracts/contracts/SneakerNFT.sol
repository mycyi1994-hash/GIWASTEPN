// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import {ERC721} from "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import {ERC721Enumerable} from "@openzeppelin/contracts/token/ERC721/extensions/ERC721Enumerable.sol";
import {ERC721Royalty} from "@openzeppelin/contracts/token/ERC721/extensions/ERC721Royalty.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";
import {EIP712} from "@openzeppelin/contracts/utils/cryptography/EIP712.sol";
import {ECDSA} from "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";
import {ERC20Burnable} from "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";

/**
 * @title SneakerNFT
 * @notice StepUp sneakers — 4 factions x 11 rarity variants = 44 designs.
 *
 * The numbers here are not new. They are the same constants the Android client
 * has been running on (`domain/Sneaker.kt`, `domain/RewardEconomy.kt`), moved
 * on-chain so that a sneaker's earning power is publicly verifiable instead of
 * asserted by our server:
 *
 *   boost(bps) = rarityBps + variant * 30 + (level - 1) * 50
 *   upgrade    = level * 100 SUP * (4 + rarity) / 4
 *   energy     = 10 + (level - 1) * 2   cells, 600 rewardable steps each
 *
 * A maxed Legendary (rarity 3, variant 1, level 30) is 1780 bps = +17.8%.
 * That ceiling is the whole pay-to-earn surface of the game and it is a
 * `constant`, not a parameter — see docs/TOKENOMICS.md §2.2.
 *
 * ## Why minting needs a signature
 *
 * Rarity is a weighted roll biased by the Luck stat. On-chain randomness that a
 * user can see before committing is not randomness, so the roll happens off
 * chain and arrives as an EIP-712 authorisation from `roller`. The user still
 * sends the transaction and still pays: the contract burns MINT_COST SUP from
 * `msg.sender`. The signature decides *what* is minted, never *whether* the
 * user paid.
 *
 * Upgrades need no signature — cost and effect are deterministic, so the owner
 * calls `upgrade` directly and the contract is the only authority involved.
 *
 * Trust assumption for this milestone: `roller` is a single key held by the
 * StepUp attester service. Moving it to a threshold signature (or a VRF) is a
 * post-testnet milestone and is listed as an open question in
 * docs/TOKENOMICS.md §9.
 */
contract SneakerNFT is ERC721, ERC721Enumerable, ERC721Royalty, EIP712, Ownable {
    // ── Game constants — mirrored from the client ────────────────────────

    /// @notice SUP burned per mint attempt.
    uint256 public constant MINT_COST = 500 ether;

    uint8 public constant FACTION_COUNT = 4;
    uint8 public constant RARITY_COUNT = 4;

    /// @notice Royalty on secondary sales, in basis points (5%).
    uint96 public constant ROYALTY_BPS = 500;

    /// @dev Boost added per variant index, in bps (0.3%).
    uint16 public constant VARIANT_BPS = 30;

    /// @dev Boost added per level above 1, in bps (0.5%).
    uint16 public constant LEVEL_BPS = 50;

    /// @dev Base energy cells at level 1, and cells gained per level.
    uint16 public constant BASE_ENERGY_CELLS = 10;
    uint16 public constant ENERGY_CELLS_PER_LEVEL = 2;

    /// @dev Rewardable steps per energy cell.
    uint16 public constant STEPS_PER_ENERGY = 600;

    // ── Storage ──────────────────────────────────────────────────────────

    struct Sneaker {
        uint8 faction; // 0 Fire, 1 Water, 2 Lightning, 3 Wind
        uint8 rarity; // 0 Common .. 3 Legendary
        uint8 variant; // index within the rarity
        uint16 level; // 1 .. maxLevel(rarity)
        uint16 luck; // milli-units, 1000 = 1.0x
        uint16 comfort; // milli-units, 1000 = 1.0x
        uint32 serial; // mint order, 1-based
    }

    mapping(uint256 tokenId => Sneaker) private _sneakers;

    /// @notice SUP token burned by mints and upgrades.
    ERC20Burnable public immutable sup;

    /// @notice Signs mint authorisations. See the contract-level note.
    address public roller;

    /// @notice Consumed mint-authorisation nonces, keyed by recipient.
    mapping(address account => mapping(uint256 nonce => bool used)) public authUsed;

    uint256 private _nextTokenId = 1;
    uint32 private _serialCounter;
    string private _baseTokenURI;

    bytes32 private constant _MINT_AUTH_TYPEHASH = keccak256(
        "MintAuth(address to,uint8 faction,uint8 rarity,uint8 variant,uint16 luck,uint16 comfort,uint256 nonce,uint256 deadline)"
    );

    struct MintAuth {
        address to;
        uint8 faction;
        uint8 rarity;
        uint8 variant;
        uint16 luck;
        uint16 comfort;
        uint256 nonce;
        uint256 deadline;
    }

    // ── Events ───────────────────────────────────────────────────────────

    event SneakerMinted(
        uint256 indexed tokenId, address indexed to, uint8 faction, uint8 rarity, uint8 variant, uint32 serial
    );
    event SneakerUpgraded(uint256 indexed tokenId, uint16 newLevel, uint256 costBurned);
    event RollerUpdated(address indexed previous, address indexed current);
    event BaseURIUpdated(string baseURI);

    // ── Errors ───────────────────────────────────────────────────────────

    error InvalidFaction(uint8 faction);
    error InvalidRarity(uint8 rarity);
    error InvalidVariant(uint8 rarity, uint8 variant);
    error AuthExpired(uint256 deadline);
    error AuthAlreadyUsed(address to, uint256 nonce);
    error BadSignature();
    error RecipientMustBeSender();
    error NotTokenOwner(uint256 tokenId);
    error MaxLevelReached(uint256 tokenId, uint16 level);

    constructor(address supToken, address treasury, address roller_, string memory baseURI_)
        ERC721("StepUp Sneaker", "SNKR")
        EIP712("StepUpSneaker", "1")
        Ownable(msg.sender)
    {
        require(supToken != address(0), "SNKR: sup is zero");
        require(treasury != address(0), "SNKR: treasury is zero");
        require(roller_ != address(0), "SNKR: roller is zero");
        sup = ERC20Burnable(supToken);
        roller = roller_;
        _baseTokenURI = baseURI_;
        _setDefaultRoyalty(treasury, ROYALTY_BPS);
    }

    // ── Pure game math — the same functions the client runs ──────────────
    //
    // Solidity has no constant arrays, so these are written as branches. They
    // are the tables from `domain/Sneaker.kt`:
    //   rarityBps    Common 0    Rare 100   Epic 200   Legendary 300
    //   variantCount        3           3          3               2   (= 11)
    //   maxLevel           10          15         20              30

    /// @dev Rarity's base earning boost, in bps.
    function _rarityBps(uint8 rarity) private pure returns (uint16) {
        if (rarity == 0) return 0;
        if (rarity == 1) return 100;
        if (rarity == 2) return 200;
        return 300;
    }

    /// @notice Visual variants available for a rarity.
    function variantCount(uint8 rarity) public pure returns (uint8) {
        if (rarity >= RARITY_COUNT) revert InvalidRarity(rarity);
        return rarity == 3 ? 2 : 3;
    }

    /// @notice Highest level a rarity can be upgraded to.
    function maxLevel(uint8 rarity) public pure returns (uint16) {
        if (rarity == 0) return 10;
        if (rarity == 1) return 15;
        if (rarity == 2) return 20;
        if (rarity == 3) return 30;
        revert InvalidRarity(rarity);
    }

    /// @notice Earning boost in basis points. Ceiling is 1780 (+17.8%).
    function boostBps(uint8 rarity, uint8 variant, uint16 level) public pure returns (uint256) {
        if (rarity >= RARITY_COUNT) revert InvalidRarity(rarity);
        uint16 lv = level == 0 ? 1 : level;
        return uint256(_rarityBps(rarity)) + uint256(variant) * VARIANT_BPS + uint256(lv - 1) * LEVEL_BPS;
    }

    /// @notice Earning boost of a specific token, in basis points.
    function boostBpsOf(uint256 tokenId) external view returns (uint256) {
        _requireOwned(tokenId);
        Sneaker memory s = _sneakers[tokenId];
        return boostBps(s.rarity, s.variant, s.level);
    }

    /// @notice SUP burned to take a sneaker from `level` to `level + 1`.
    /// @dev level * 100 SUP * (4 + rarity) / 4 — x1.00 / x1.25 / x1.50 / x1.75.
    function upgradeCost(uint8 rarity, uint16 level) public pure returns (uint256) {
        if (rarity >= RARITY_COUNT) revert InvalidRarity(rarity);
        return (uint256(level) * 100 ether * (4 + uint256(rarity))) / 4;
    }

    /// @notice Daily energy cells at a level. Caps rewardable steps, not the rate.
    function energyCells(uint16 level) public pure returns (uint256) {
        uint16 lv = level == 0 ? 1 : level;
        return uint256(BASE_ENERGY_CELLS) + uint256(lv - 1) * ENERGY_CELLS_PER_LEVEL;
    }

    /// @notice Daily rewardable step cap at a level, ignoring the Comfort discount.
    function rewardableSteps(uint16 level) external pure returns (uint256) {
        return energyCells(level) * STEPS_PER_ENERGY;
    }

    // ── Minting ──────────────────────────────────────────────────────────

    /// @notice Hash a mint authorisation for off-chain signing.
    function hashMintAuth(MintAuth calldata a) public view returns (bytes32) {
        return _hashTypedDataV4(
            keccak256(
                abi.encode(
                    _MINT_AUTH_TYPEHASH, a.to, a.faction, a.rarity, a.variant, a.luck, a.comfort, a.nonce, a.deadline
                )
            )
        );
    }

    /**
     * @notice Burn MINT_COST SUP and mint the authorised sneaker.
     * @dev Caller must have approved this contract for MINT_COST SUP.
     */
    function mintWithAuth(MintAuth calldata a, bytes calldata signature) external returns (uint256 tokenId) {
        if (a.to != msg.sender) revert RecipientMustBeSender();
        if (block.timestamp > a.deadline) revert AuthExpired(a.deadline);
        if (authUsed[a.to][a.nonce]) revert AuthAlreadyUsed(a.to, a.nonce);
        if (a.faction >= FACTION_COUNT) revert InvalidFaction(a.faction);
        if (a.rarity >= RARITY_COUNT) revert InvalidRarity(a.rarity);
        if (a.variant >= variantCount(a.rarity)) revert InvalidVariant(a.rarity, a.variant);

        address signer = ECDSA.recover(hashMintAuth(a), signature);
        if (signer != roller) revert BadSignature();

        authUsed[a.to][a.nonce] = true;

        // Burn first, mint second — a failed burn must not leave an NFT behind.
        sup.burnFrom(msg.sender, MINT_COST);

        tokenId = _nextTokenId++;
        _serialCounter += 1;
        _sneakers[tokenId] = Sneaker({
            faction: a.faction,
            rarity: a.rarity,
            variant: a.variant,
            level: 1,
            luck: a.luck,
            comfort: a.comfort,
            serial: _serialCounter
        });

        _safeMint(a.to, tokenId);
        emit SneakerMinted(tokenId, a.to, a.faction, a.rarity, a.variant, _serialCounter);
    }

    // ── Upgrading ────────────────────────────────────────────────────────

    /**
     * @notice Burn SUP to raise a sneaker one level. Owner-only, no signature.
     * @dev Caller must have approved this contract for `upgradeCostOf(tokenId)`.
     */
    function upgrade(uint256 tokenId) external returns (uint16 newLevel) {
        if (ownerOf(tokenId) != msg.sender) revert NotTokenOwner(tokenId);
        Sneaker storage s = _sneakers[tokenId];
        if (s.level >= maxLevel(s.rarity)) revert MaxLevelReached(tokenId, s.level);

        uint256 cost = upgradeCost(s.rarity, s.level);
        sup.burnFrom(msg.sender, cost);

        s.level += 1;
        newLevel = s.level;
        emit SneakerUpgraded(tokenId, newLevel, cost);
    }

    /// @notice SUP required for this token's next upgrade. Reverts at max level.
    function upgradeCostOf(uint256 tokenId) external view returns (uint256) {
        _requireOwned(tokenId);
        Sneaker memory s = _sneakers[tokenId];
        if (s.level >= maxLevel(s.rarity)) revert MaxLevelReached(tokenId, s.level);
        return upgradeCost(s.rarity, s.level);
    }

    // ── Views ────────────────────────────────────────────────────────────

    function sneakerOf(uint256 tokenId) external view returns (Sneaker memory) {
        _requireOwned(tokenId);
        return _sneakers[tokenId];
    }

    /// @notice Total sneakers ever minted, including any later burned.
    function totalMinted() external view returns (uint32) {
        return _serialCounter;
    }

    // ── Admin ────────────────────────────────────────────────────────────

    function setRoller(address roller_) external onlyOwner {
        require(roller_ != address(0), "SNKR: roller is zero");
        emit RollerUpdated(roller, roller_);
        roller = roller_;
    }

    function setBaseURI(string calldata baseURI_) external onlyOwner {
        _baseTokenURI = baseURI_;
        emit BaseURIUpdated(baseURI_);
    }

    function setRoyaltyReceiver(address receiver) external onlyOwner {
        require(receiver != address(0), "SNKR: receiver is zero");
        _setDefaultRoyalty(receiver, ROYALTY_BPS);
    }

    // ── Overrides ────────────────────────────────────────────────────────

    function _baseURI() internal view override returns (string memory) {
        return _baseTokenURI;
    }

    function _update(address to, uint256 tokenId, address auth)
        internal
        override(ERC721, ERC721Enumerable)
        returns (address)
    {
        return super._update(to, tokenId, auth);
    }

    function _increaseBalance(address account, uint128 value) internal override(ERC721, ERC721Enumerable) {
        super._increaseBalance(account, value);
    }

    function supportsInterface(bytes4 interfaceId)
        public
        view
        override(ERC721, ERC721Enumerable, ERC721Royalty)
        returns (bool)
    {
        return super.supportsInterface(interfaceId);
    }
}
