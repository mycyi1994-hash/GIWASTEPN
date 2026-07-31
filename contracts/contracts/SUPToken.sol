// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {ERC20Burnable} from "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";
import {ERC20Permit} from "@openzeppelin/contracts/token/ERC20/extensions/ERC20Permit.sol";

/**
 * @title SUPToken
 * @notice StepUp's reward and utility token on the GIWA chain.
 *
 * Fixed supply. The entire 1,000,000,000 SUP is minted once, in the
 * constructor, to the treasury address. There is no mint function and no
 * minter role — after deployment the supply can only ever go down, through
 * `burn` / `burnFrom` (sneaker minting, upgrades and boost purchases all
 * burn).
 *
 * This is deliberately the least interesting contract in the system. A reward
 * token whose supply can be changed later is not a cap; it is a promise. The
 * only way to make the cap real is to leave no code path that raises it.
 *
 * See docs/TOKENOMICS.md §6 for the allocation the treasury distributes.
 */
contract SUPToken is ERC20, ERC20Burnable, ERC20Permit {
    /// @notice Hard cap. Minted in full at deployment; never increases.
    uint256 public constant TOTAL_SUPPLY = 1_000_000_000 ether;

    /**
     * @param treasury Receives the entire supply at deployment. Responsible for
     *        funding the RewardDistributor pool and the vesting schedules.
     */
    constructor(address treasury) ERC20("StepUp", "SUP") ERC20Permit("StepUp") {
        require(treasury != address(0), "SUP: treasury is zero");
        _mint(treasury, TOTAL_SUPPLY);
    }
}
