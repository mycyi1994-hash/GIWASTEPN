const { expect } = require("chai");
const { ethers } = require("hardhat");
const { time } = require("@nomicfoundation/hardhat-network-helpers");

const DAY = 24 * 60 * 60;
const eth = (n) => ethers.parseEther(String(n));

// ── EIP-712 helpers — the same payloads the attester service will sign ────

async function signMintAuth(roller, sneakerAddress, auth) {
  const { chainId } = await ethers.provider.getNetwork();
  return roller.signTypedData(
    { name: "StepUpSneaker", version: "1", chainId, verifyingContract: sneakerAddress },
    {
      MintAuth: [
        { name: "to", type: "address" },
        { name: "faction", type: "uint8" },
        { name: "rarity", type: "uint8" },
        { name: "variant", type: "uint8" },
        { name: "luck", type: "uint16" },
        { name: "comfort", type: "uint16" },
        { name: "nonce", type: "uint256" },
        { name: "deadline", type: "uint256" },
      ],
    },
    auth,
  );
}

async function signClaim(attester, distributorAddress, claim) {
  const { chainId } = await ethers.provider.getNetwork();
  return attester.signTypedData(
    { name: "StepUpRewards", version: "1", chainId, verifyingContract: distributorAddress },
    {
      Claim: [
        { name: "runner", type: "address" },
        { name: "sessionHash", type: "bytes32" },
        { name: "amount", type: "uint256" },
        { name: "day", type: "uint64" },
        { name: "deadline", type: "uint256" },
      ],
    },
    claim,
  );
}

async function deployAll() {
  const [deployer, treasury, attester, roller, runner, other] = await ethers.getSigners();

  const sup = await (await ethers.getContractFactory("SUPToken")).deploy(treasury.address);
  const supAddr = await sup.getAddress();

  const sneaker = await (
    await ethers.getContractFactory("SneakerNFT")
  ).deploy(supAddr, treasury.address, roller.address, "ipfs://cid/");

  const distributor = await (
    await ethers.getContractFactory("RewardDistributor")
  ).deploy(supAddr, attester.address);

  const registry = await (await ethers.getContractFactory("CourseRegistry")).deploy(attester.address);

  return { deployer, treasury, attester, roller, runner, other, sup, sneaker, distributor, registry };
}

// ─────────────────────────────────────────────────────────────────────────

describe("SUPToken", () => {
  it("mints the entire fixed supply to the treasury", async () => {
    const { sup, treasury } = await deployAll();
    expect(await sup.TOTAL_SUPPLY()).to.equal(eth(1_000_000_000));
    expect(await sup.totalSupply()).to.equal(eth(1_000_000_000));
    expect(await sup.balanceOf(treasury.address)).to.equal(eth(1_000_000_000));
  });

  it("has no way to mint more — supply only ever falls", async () => {
    const { sup, treasury } = await deployAll();
    expect(sup.interface.fragments.some((f) => f.type === "function" && f.name === "mint")).to.equal(false);

    await sup.connect(treasury).burn(eth(1000));
    expect(await sup.totalSupply()).to.equal(eth(1_000_000_000) - eth(1000));
  });

  it("rejects a zero treasury", async () => {
    const SUPToken = await ethers.getContractFactory("SUPToken");
    await expect(SUPToken.deploy(ethers.ZeroAddress)).to.be.revertedWith("SUP: treasury is zero");
  });
});

describe("SneakerNFT — game math matches the Android client", () => {
  it("reproduces the boost table from domain/Sneaker.kt", async () => {
    const { sneaker } = await deployAll();
    // rarity, maxVariantIndex, maxLevel -> expected bps
    const cases = [
      [0, 2, 10, 510], // Common     +5.1%
      [1, 2, 15, 860], // Rare       +8.6%
      [2, 2, 20, 1210], // Epic      +12.1%
      [3, 1, 30, 1780], // Legendary +17.8%  ← the ceiling
    ];
    for (const [rarity, variant, level, expected] of cases) {
      expect(await sneaker.boostBps(rarity, variant, level)).to.equal(expected);
    }
    // A free Common at Lv.1 is the floor.
    expect(await sneaker.boostBps(0, 0, 1)).to.equal(0);
  });

  it("caps the whole collection at +17.8%", async () => {
    const { sneaker } = await deployAll();
    let ceiling = 0n;
    for (let rarity = 0; rarity < 4; rarity++) {
      const variants = await sneaker.variantCount(rarity);
      const max = await sneaker.maxLevel(rarity);
      const bps = await sneaker.boostBps(rarity, Number(variants) - 1, max);
      if (bps > ceiling) ceiling = bps;
    }
    expect(ceiling).to.equal(1780n);
  });

  it("has 11 variants per faction — 44 designs in total", async () => {
    const { sneaker } = await deployAll();
    let total = 0;
    for (let rarity = 0; rarity < 4; rarity++) total += Number(await sneaker.variantCount(rarity));
    expect(total).to.equal(11);
    expect(total * Number(await sneaker.FACTION_COUNT())).to.equal(44);
  });

  it("reproduces the upgrade cost table", async () => {
    const { sneaker } = await deployAll();
    expect(await sneaker.upgradeCost(0, 1)).to.equal(eth(100));
    expect(await sneaker.upgradeCost(1, 1)).to.equal(eth(125));
    expect(await sneaker.upgradeCost(2, 1)).to.equal(eth(150));
    expect(await sneaker.upgradeCost(3, 1)).to.equal(eth(175));

    // Full path to each rarity's max level — the numbers published in TOKENOMICS §3.
    const totals = { 0: [10, 4500], 1: [15, 13125], 2: [20, 28500], 3: [30, 76125] };
    for (const [rarity, [max, expected]] of Object.entries(totals)) {
      let sum = 0n;
      for (let level = 1; level < max; level++) sum += await sneaker.upgradeCost(rarity, level);
      expect(sum).to.equal(eth(expected));
    }
  });

  it("scales the daily step cap with level, not the per-step rate", async () => {
    const { sneaker } = await deployAll();
    expect(await sneaker.rewardableSteps(1)).to.equal(6000);
    expect(await sneaker.rewardableSteps(5)).to.equal(10800);
    expect(await sneaker.rewardableSteps(15)).to.equal(22800);
    expect(await sneaker.rewardableSteps(30)).to.equal(40800);
  });
});

describe("SneakerNFT — minting", () => {
  async function fixture() {
    const ctx = await deployAll();
    const { sup, treasury, runner, sneaker } = ctx;
    await sup.connect(treasury).transfer(runner.address, eth(100_000));
    await sup.connect(runner).approve(await sneaker.getAddress(), eth(100_000));
    return ctx;
  }

  const authFor = async (runner, overrides = {}) => ({
    to: runner.address,
    faction: 2,
    rarity: 3,
    variant: 1,
    luck: 1450,
    comfort: 1200,
    nonce: 1,
    deadline: (await time.latest()) + 3600,
    ...overrides,
  });

  it("burns 500 SUP and mints the authorised sneaker", async () => {
    const { sneaker, sup, roller, runner } = await fixture();
    const addr = await sneaker.getAddress();
    const auth = await authFor(runner);
    const sig = await signMintAuth(roller, addr, auth);

    const supplyBefore = await sup.totalSupply();
    await expect(sneaker.connect(runner).mintWithAuth(auth, sig))
      .to.emit(sneaker, "SneakerMinted")
      .withArgs(1n, runner.address, 2, 3, 1, 1);

    expect(await sup.totalSupply()).to.equal(supplyBefore - eth(500));
    expect(await sneaker.ownerOf(1)).to.equal(runner.address);

    const s = await sneaker.sneakerOf(1);
    expect([s.faction, s.rarity, s.variant, s.level, s.luck, s.comfort, s.serial]).to.deep.equal([
      2, 3, 1, 1n, 1450n, 1200n, 1n,
    ]);
    expect(await sneaker.boostBpsOf(1)).to.equal(330n); // 300 + 1*30 + 0
  });

  it("rejects a signature from anyone but the roller", async () => {
    const { sneaker, runner, other } = await fixture();
    const addr = await sneaker.getAddress();
    const auth = await authFor(runner);
    const sig = await signMintAuth(other, addr, auth);
    await expect(sneaker.connect(runner).mintWithAuth(auth, sig)).to.be.revertedWithCustomError(
      sneaker,
      "BadSignature",
    );
  });

  it("rejects a tampered roll — you cannot upgrade your own rarity", async () => {
    const { sneaker, roller, runner } = await fixture();
    const addr = await sneaker.getAddress();
    const auth = await authFor(runner, { rarity: 0, variant: 0 });
    const sig = await signMintAuth(roller, addr, auth);
    const tampered = { ...auth, rarity: 3, variant: 1 };
    await expect(sneaker.connect(runner).mintWithAuth(tampered, sig)).to.be.revertedWithCustomError(
      sneaker,
      "BadSignature",
    );
  });

  it("consumes the nonce — one authorisation, one sneaker", async () => {
    const { sneaker, roller, runner } = await fixture();
    const addr = await sneaker.getAddress();
    const auth = await authFor(runner);
    const sig = await signMintAuth(roller, addr, auth);

    await sneaker.connect(runner).mintWithAuth(auth, sig);
    await expect(sneaker.connect(runner).mintWithAuth(auth, sig))
      .to.be.revertedWithCustomError(sneaker, "AuthAlreadyUsed")
      .withArgs(runner.address, 1n);
  });

  it("rejects an expired authorisation", async () => {
    const { sneaker, roller, runner } = await fixture();
    const addr = await sneaker.getAddress();
    const auth = await authFor(runner, { deadline: (await time.latest()) + 60 });
    const sig = await signMintAuth(roller, addr, auth);
    await time.increase(120);
    await expect(sneaker.connect(runner).mintWithAuth(auth, sig)).to.be.revertedWithCustomError(
      sneaker,
      "AuthExpired",
    );
  });

  it("rejects a variant that does not exist for the rarity", async () => {
    const { sneaker, roller, runner } = await fixture();
    const addr = await sneaker.getAddress();
    // Legendary only has variants 0 and 1.
    const auth = await authFor(runner, { rarity: 3, variant: 2 });
    const sig = await signMintAuth(roller, addr, auth);
    await expect(sneaker.connect(runner).mintWithAuth(auth, sig))
      .to.be.revertedWithCustomError(sneaker, "InvalidVariant")
      .withArgs(3, 2);
  });

  it("will not mint for someone else's authorisation", async () => {
    const { sneaker, roller, runner, other } = await fixture();
    const addr = await sneaker.getAddress();
    const auth = await authFor(runner);
    const sig = await signMintAuth(roller, addr, auth);
    await expect(sneaker.connect(other).mintWithAuth(auth, sig)).to.be.revertedWithCustomError(
      sneaker,
      "RecipientMustBeSender",
    );
  });

  it("reverts the mint when the runner cannot pay", async () => {
    const { sneaker, roller, other } = await fixture(); // `other` has no SUP
    const addr = await sneaker.getAddress();
    const auth = await authFor(other);
    const sig = await signMintAuth(roller, addr, auth);
    await expect(sneaker.connect(other).mintWithAuth(auth, sig)).to.be.reverted;
    expect(await sneaker.totalMinted()).to.equal(0);
  });
});

describe("SneakerNFT — upgrading", () => {
  async function minted(rarity = 0, variant = 0) {
    const ctx = await deployAll();
    const { sup, treasury, runner, sneaker, roller } = ctx;
    await sup.connect(treasury).transfer(runner.address, eth(200_000));
    await sup.connect(runner).approve(await sneaker.getAddress(), eth(200_000));
    const auth = {
      to: runner.address,
      faction: 0,
      rarity,
      variant,
      luck: 1000,
      comfort: 1000,
      nonce: 7,
      deadline: (await time.latest()) + 3600,
    };
    const sig = await signMintAuth(roller, await sneaker.getAddress(), auth);
    await sneaker.connect(runner).mintWithAuth(auth, sig);
    return ctx;
  }

  it("burns the exact cost and raises the level by one", async () => {
    const { sneaker, sup, runner } = await minted(1);
    const cost = await sneaker.upgradeCostOf(1);
    expect(cost).to.equal(eth(125)); // Rare, level 1

    const supplyBefore = await sup.totalSupply();
    await expect(sneaker.connect(runner).upgrade(1)).to.emit(sneaker, "SneakerUpgraded").withArgs(1n, 2n, cost);
    expect(await sup.totalSupply()).to.equal(supplyBefore - cost);
    expect((await sneaker.sneakerOf(1)).level).to.equal(2n);
    expect(await sneaker.boostBpsOf(1)).to.equal(150n); // 100 + 0 + 50
  });

  it("stops at the rarity's max level", async () => {
    const { sneaker, runner } = await minted(0); // Common, max 10
    for (let i = 1; i < 10; i++) await sneaker.connect(runner).upgrade(1);
    expect((await sneaker.sneakerOf(1)).level).to.equal(10n);
    await expect(sneaker.connect(runner).upgrade(1))
      .to.be.revertedWithCustomError(sneaker, "MaxLevelReached")
      .withArgs(1n, 10n);
  });

  it("only the owner can upgrade", async () => {
    const { sneaker, other } = await minted(0);
    await expect(sneaker.connect(other).upgrade(1)).to.be.revertedWithCustomError(sneaker, "NotTokenOwner");
  });

  it("charges 4,500 SUP to take a Common to level 10", async () => {
    const { sneaker, sup, runner } = await minted(0);
    const before = await sup.balanceOf(runner.address);
    for (let i = 1; i < 10; i++) await sneaker.connect(runner).upgrade(1);
    expect(before - (await sup.balanceOf(runner.address))).to.equal(eth(4500));
  });
});

describe("RewardDistributor — emission schedule", () => {
  it("halves the daily budget every 730 days", async () => {
    const { distributor } = await deployAll();
    expect(await distributor.dailyBudget(0)).to.equal(eth(250_000));
    expect(await distributor.dailyBudget(729)).to.equal(eth(250_000));
    expect(await distributor.dailyBudget(730)).to.equal(eth(125_000));
    expect(await distributor.dailyBudget(1460)).to.equal(eth(62_500));
    expect(await distributor.dailyBudget(2190)).to.equal(eth(31_250));
    expect(await distributor.dailyBudget(2920)).to.equal(eth(15_625));
  });

  it("converges to 365,000,000 SUP over the full schedule", async () => {
    const { distributor } = await deployAll();
    let total = 0n;
    for (let epoch = 0; epoch < 40; epoch++) {
      total += (await distributor.dailyBudget(epoch * 730)) * 730n;
    }
    // The geometric series limit is 2 x the first epoch = 365,000,000 SUP.
    expect(total).to.be.lessThanOrEqual(eth(365_000_000));
    expect(total).to.be.greaterThan(eth(364_999_000));
  });

  it("runs dry rather than overflowing at absurd day numbers", async () => {
    const { distributor } = await deployAll();
    expect(await distributor.dailyBudget(730n * 64n)).to.equal(0);
  });
});

describe("RewardDistributor — claiming", () => {
  async function funded() {
    const ctx = await deployAll();
    const { sup, treasury, distributor } = ctx;
    const addr = await distributor.getAddress();
    await sup.connect(treasury).approve(addr, eth(50_000_000));
    await distributor.connect(treasury).fund(eth(50_000_000));
    return ctx;
  }

  const claimFor = async (runner, overrides = {}) => ({
    runner: runner.address,
    sessionHash: ethers.id("session-1"),
    amount: eth(120),
    day: 0,
    deadline: (await time.latest()) + 3600,
    ...overrides,
  });

  it("pays an attested run and records it", async () => {
    const { distributor, sup, attester, runner } = await funded();
    const addr = await distributor.getAddress();
    const c = await claimFor(runner);
    const sig = await signClaim(attester, addr, c);

    await expect(distributor.claim(c, sig))
      .to.emit(distributor, "Claimed")
      .withArgs(runner.address, c.sessionHash, eth(120), 0n, eth(250_000) - eth(120));

    expect(await sup.balanceOf(runner.address)).to.equal(eth(120));
    expect(await distributor.totalDistributed()).to.equal(eth(120));
    expect(await distributor.claimedBy(runner.address)).to.equal(eth(120));
    expect(await distributor.dayRemaining(0)).to.equal(eth(250_000) - eth(120));
  });

  it("pays the runner even when a relayer submits — no gas needed to be paid", async () => {
    const { distributor, sup, attester, runner, other } = await funded();
    const c = await claimFor(runner);
    const sig = await signClaim(attester, await distributor.getAddress(), c);

    await distributor.connect(other).claim(c, sig);
    expect(await sup.balanceOf(runner.address)).to.equal(eth(120));
    expect(await sup.balanceOf(other.address)).to.equal(0);
  });

  it("refuses to pay the same session twice", async () => {
    const { distributor, attester, runner } = await funded();
    const c = await claimFor(runner);
    const sig = await signClaim(attester, await distributor.getAddress(), c);

    await distributor.claim(c, sig);
    await expect(distributor.claim(c, sig))
      .to.be.revertedWithCustomError(distributor, "SessionAlreadyClaimed")
      .withArgs(c.sessionHash);
  });

  it("refuses a signature from anyone but the attester", async () => {
    const { distributor, other, runner } = await funded();
    const c = await claimFor(runner);
    const sig = await signClaim(other, await distributor.getAddress(), c);
    await expect(distributor.claim(c, sig)).to.be.revertedWithCustomError(distributor, "BadSignature");
  });

  it("caps the day even for a validly signed claim — the attester cannot inflate", async () => {
    const { distributor, attester, runner } = await funded();
    const addr = await distributor.getAddress();

    const big = await claimFor(runner, { sessionHash: ethers.id("s-big"), amount: eth(249_900) });
    await distributor.claim(big, await signClaim(attester, addr, big));
    expect(await distributor.dayRemaining(0)).to.equal(eth(100));

    const over = await claimFor(runner, { sessionHash: ethers.id("s-over"), amount: eth(101) });
    await expect(distributor.claim(over, await signClaim(attester, addr, over)))
      .to.be.revertedWithCustomError(distributor, "DailyBudgetExceeded")
      .withArgs(0n, eth(101), eth(100));

    // The remainder is still claimable — the cap is a ceiling, not a lockout.
    const fits = await claimFor(runner, { sessionHash: ethers.id("s-fits"), amount: eth(100) });
    await distributor.claim(fits, await signClaim(attester, addr, fits));
    expect(await distributor.dayRemaining(0)).to.equal(0);
  });

  it("gives each new day a fresh budget", async () => {
    const { distributor, attester, runner } = await funded();
    const addr = await distributor.getAddress();

    const d0 = await claimFor(runner, { sessionHash: ethers.id("d0"), amount: eth(250_000) });
    await distributor.claim(d0, await signClaim(attester, addr, d0));
    expect(await distributor.dayRemaining(0)).to.equal(0);

    await time.increase(DAY);
    expect(await distributor.currentDay()).to.equal(1n);

    const d1 = await claimFor(runner, {
      sessionHash: ethers.id("d1"),
      amount: eth(250_000),
      day: 1,
      deadline: (await time.latest()) + 3600,
    });
    await distributor.claim(d1, await signClaim(attester, addr, d1));
    expect(await distributor.dayRemaining(1)).to.equal(0);
  });

  it("rejects a day in the future", async () => {
    const { distributor, attester, runner } = await funded();
    const c = await claimFor(runner, { day: 5 });
    const sig = await signClaim(attester, await distributor.getAddress(), c);
    await expect(distributor.claim(c, sig)).to.be.revertedWithCustomError(distributor, "DayInFuture");
  });

  it("rejects a session older than the claim window", async () => {
    const { distributor, attester, runner } = await funded();
    await time.increase(DAY * 9);
    const c = await claimFor(runner, { day: 0, deadline: (await time.latest()) + 3600 });
    const sig = await signClaim(attester, await distributor.getAddress(), c);
    await expect(distributor.claim(c, sig)).to.be.revertedWithCustomError(distributor, "DayTooOld");
  });

  it("rejects an expired claim", async () => {
    const { distributor, attester, runner } = await funded();
    const c = await claimFor(runner, { deadline: (await time.latest()) + 60 });
    const sig = await signClaim(attester, await distributor.getAddress(), c);
    await time.increase(120);
    await expect(distributor.claim(c, sig)).to.be.revertedWithCustomError(distributor, "ClaimExpired");
  });

  it("stops paying when the pool is empty, before the budget is spent", async () => {
    const ctx = await deployAll(); // deliberately unfunded
    const { distributor, attester, runner } = ctx;
    const c = await claimFor(runner);
    const sig = await signClaim(attester, await distributor.getAddress(), c);
    await expect(distributor.claim(c, sig))
      .to.be.revertedWithCustomError(distributor, "PoolExhausted")
      .withArgs(eth(120), 0n);
  });

  it("can be paused, and only by the owner", async () => {
    const { distributor, attester, runner, other } = await funded();
    await expect(distributor.connect(other).pause()).to.be.revertedWithCustomError(
      distributor,
      "OwnableUnauthorizedAccount",
    );
    await distributor.pause();
    const c = await claimFor(runner);
    const sig = await signClaim(attester, await distributor.getAddress(), c);
    await expect(distributor.claim(c, sig)).to.be.revertedWithCustomError(distributor, "EnforcedPause");

    await distributor.unpause();
    await distributor.claim(c, sig);
  });

  it("has no owner path to the pool — SUP can only leave via claim", async () => {
    const { distributor } = await deployAll();
    const escapeHatches = distributor.interface.fragments
      .filter((f) => f.type === "function")
      .map((f) => f.name)
      .filter((n) => /withdraw|sweep|rescue|recover|drain|transferOut/i.test(n));
    expect(escapeHatches).to.deep.equal([]);
  });

  it("lets the owner rotate a lost attester key", async () => {
    const { distributor, attester, other, runner } = await funded();
    await distributor.setAttester(other.address);
    const c = await claimFor(runner);

    const oldSig = await signClaim(attester, await distributor.getAddress(), c);
    await expect(distributor.claim(c, oldSig)).to.be.revertedWithCustomError(distributor, "BadSignature");

    const newSig = await signClaim(other, await distributor.getAddress(), c);
    await distributor.claim(c, newSig);
  });
});

describe("CourseRegistry", () => {
  const HASH = ethers.id("37.5299,126.9648;37.5301,126.9655");

  it("pays km x 1.0 SUP, capped at a marathon", async () => {
    const { registry } = await deployAll();
    expect(await registry.rewardFor(5000)).to.equal(eth(5)); // 5.00 km
    expect(await registry.rewardFor(4680)).to.equal(eth(4.68)); // 여의도 seed course
    expect(await registry.rewardFor(42000)).to.equal(eth(42));
    expect(await registry.rewardFor(100000)).to.equal(eth(42)); // capped
    expect(await registry.rewardFor(0)).to.equal(0);
  });

  it("requires 98% of the course to count as finished", async () => {
    const { registry } = await deployAll();
    expect(await registry.requiredDistanceM(5000)).to.equal(4900);
  });

  it("records authorship, and anyone may author", async () => {
    const { registry, runner } = await deployAll();
    await expect(registry.connect(runner).createCourse("한강 야간 코스", 5000, 42, HASH))
      .to.emit(registry, "CourseCreated")
      .withArgs(1n, runner.address, "한강 야간 코스", 5000, HASH);

    const c = await registry.courseOf(1);
    expect(c.author).to.equal(runner.address);
    expect(c.distanceM).to.equal(5000);
    expect(c.polylineHash).to.equal(HASH);
    expect(await registry.coursesByAuthor(runner.address)).to.deep.equal([1n]);
  });

  it("rejects courses that are too short, unnamed, or untracked", async () => {
    const { registry, runner } = await deployAll();
    await expect(registry.connect(runner).createCourse("x", 100, 0, HASH)).to.be.revertedWithCustomError(
      registry,
      "DistanceTooShort",
    );
    await expect(registry.connect(runner).createCourse("", 5000, 0, HASH)).to.be.revertedWithCustomError(
      registry,
      "EmptyName",
    );
    await expect(
      registry.connect(runner).createCourse("no track", 5000, 0, ethers.ZeroHash),
    ).to.be.revertedWithCustomError(registry, "EmptyPolylineHash");
  });

  it("only the recorder may credit completions", async () => {
    const { registry, attester, runner, other } = await deployAll();
    await registry.connect(runner).createCourse("반포", 4100, 12, HASH);

    await expect(registry.connect(other).recordCompletion(1, runner.address)).to.be.revertedWithCustomError(
      registry,
      "NotRecorder",
    );

    await expect(registry.connect(attester).recordCompletion(1, runner.address))
      .to.emit(registry, "CourseCompleted")
      .withArgs(1n, runner.address, 1n, eth(4.1));

    expect((await registry.courseOf(1)).runCount).to.equal(1);
    expect(await registry.completionsBy(1, runner.address)).to.equal(1);
  });

  it("keeps the record when a course is retired", async () => {
    const { registry, attester, runner, other } = await deployAll();
    await registry.connect(runner).createCourse("남산", 2730, 90, HASH);
    await registry.connect(attester).recordCompletion(1, runner.address);

    await expect(registry.connect(other).retireCourse(1)).to.be.revertedWithCustomError(registry, "NotCourseAuthor");
    await registry.connect(runner).retireCourse(1);

    const c = await registry.courseOf(1);
    expect(c.retired).to.equal(true);
    expect(c.author).to.equal(runner.address); // authorship survives
    expect(c.runCount).to.equal(1); // so does the history

    await expect(registry.connect(attester).recordCompletion(1, runner.address)).to.be.revertedWithCustomError(
      registry,
      "AlreadyRetired",
    );
  });

  it("rejects an unknown course", async () => {
    const { registry } = await deployAll();
    await expect(registry.courseOf(99)).to.be.revertedWithCustomError(registry, "UnknownCourse");
  });
});
