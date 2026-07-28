package com.giwa.strideup.domain

import kotlin.random.Random

/**
 * StrideUp 스니커즈 NFT — **속성(Faction) × 등급(Rarity) × 변형(Variant)** 체계.
 *
 * 속성 4개(불·물·번개·바람) × 등급별 변형 11종 = 총 44종.
 * 속성이 색과 이펙트를, 등급이 실루엣 정교함·오너먼트·부스트를 정한다.
 *
 * 부스트는 의도적으로 작게 잡았다 — 등급이 한 단계 오를 때마다 +1%.
 * 수집의 재미는 성능 격차가 아니라 외형과 도감 완성에서 나온다.
 */

// ─────────────────────────────────────────────────────────────
// 속성
// ─────────────────────────────────────────────────────────────

enum class Faction(
    val id: String,
    val displayName: String,
    /** 주 네온색 */
    val accent: Int,
    /** 밝은 하이라이트 */
    val accentSoft: Int,
    /** 음영 */
    val accentDeep: Int,
    /** 어퍼 기본색 */
    val upper: Int,
    /** 어퍼 음영 */
    val upperShade: Int,
    /** 밑창색 */
    val sole: Int,
) {
    FIRE(
        "FIRE", "Fire",
        accent = 0xFFFF2E2E.toInt(),
        accentSoft = 0xFFFF9166.toInt(),
        accentDeep = 0xFF9E1010.toInt(),
        upper = 0xFF17100F.toInt(),
        upperShade = 0xFF0D0808.toInt(),
        sole = 0xFF241416.toInt(),
    ),
    WATER(
        "WATER", "Water",
        accent = 0xFF2E9BFF.toInt(),
        accentSoft = 0xFFA6E4FF.toInt(),
        accentDeep = 0xFF0F4FA8.toInt(),
        upper = 0xFF0F151E.toInt(),
        upperShade = 0xFF080C12.toInt(),
        sole = 0xFF15202E.toInt(),
    ),
    LIGHTNING(
        "LIGHTNING", "Lightning",
        accent = 0xFFF5E800.toInt(),
        accentSoft = 0xFFFFFA9E.toInt(),
        accentDeep = 0xFFA89C00.toInt(),
        upper = 0xFF15140D.toInt(),
        upperShade = 0xFF0B0A06.toInt(),
        sole = 0xFF201E12.toInt(),
    ),
    WIND(
        "WIND", "Wind",
        accent = 0xFFA4F515.toInt(),
        accentSoft = 0xFFDCFF8F.toInt(),
        accentDeep = 0xFF5F9400.toInt(),
        upper = 0xFF10150C.toInt(),
        upperShade = 0xFF080B06.toInt(),
        sole = 0xFF182014.toInt(),
    );

    companion object {
        fun of(id: String): Faction = entries.firstOrNull { it.id == id } ?: FIRE
    }
}

// ─────────────────────────────────────────────────────────────
// 등급
// ─────────────────────────────────────────────────────────────

enum class Rarity(
    val id: String,
    /** 이 등급에 속한 변형 개수 */
    val variantCount: Int,
    /** 적립 부스트 (%) — 등급 한 단계당 +1% */
    val boostPercent: Double,
    val maxLevel: Int,
    /** 민팅 가중치 */
    val weight: Int,
) {
    COMMON("COMMON", variantCount = 3, boostPercent = 0.0, maxLevel = 10, weight = 55),
    RARE("RARE", variantCount = 3, boostPercent = 1.0, maxLevel = 15, weight = 28),
    EPIC("EPIC", variantCount = 3, boostPercent = 2.0, maxLevel = 20, weight = 13),
    LEGENDARY("LEGENDARY", variantCount = 2, boostPercent = 3.0, maxLevel = 30, weight = 4);

    companion object {
        fun of(id: String): Rarity = entries.firstOrNull { it.id == id } ?: COMMON

        /** 가중 추첨. luck이 높을수록 상위 등급 가중치가 커진다. */
        fun roll(random: Random, luck: Double = 0.0): Rarity {
            val weights = entries.map { r ->
                r to r.weight * (1.0 + luck * 0.15 * r.ordinal)
            }
            val total = weights.sumOf { it.second }
            var pick = random.nextDouble() * total
            for ((rarity, w) in weights) {
                pick -= w
                if (pick <= 0) return rarity
            }
            return COMMON
        }
    }
}

/** 속성 하나가 가진 변형 개수 (3+3+3+2 = 11) */
val VARIANTS_PER_FACTION: Int = Rarity.entries.sumOf { it.variantCount }

/** 전체 도감 크기 = 속성 4 × 변형 11 */
val TOTAL_COLLECTION: Int = Faction.entries.size * VARIANTS_PER_FACTION

// ─────────────────────────────────────────────────────────────
// 실루엣
// ─────────────────────────────────────────────────────────────

enum class StripeStyle { SWOOSH, BLADE, CHEVRON, DUAL, WAVE, SPLIT }

/**
 * 등급·변형마다 다른 신발 형태.
 * 상위 등급일수록 밑창이 두껍고 발광 포드가 늘어나며 하이탑이 섞인다.
 */
data class Silhouette(
    val soleThickness: Float,
    val toeRise: Float,
    val collarTop: Float,
    val stripe: StripeStyle,
    /** 미드솔 발광 포드 개수 */
    val pods: Int,
    val highTop: Boolean,
)

/**
 * 등급·변형별 모델명. 속성명과 합쳐 "Fire Apex" 같은 이름이 된다.
 * 브랜드명 성격이라 현지화하지 않는다.
 */
object VariantNames {
    private val common = listOf("Runner", "Trainer", "Trail")
    private val rare = listOf("Racer", "Glide", "Blade")
    private val epic = listOf("Apex", "Phantom", "Titan")
    private val legendary = listOf("Seraph", "Dragon")

    fun of(rarity: Rarity, variant: Int): String {
        val list = when (rarity) {
            Rarity.COMMON -> common
            Rarity.RARE -> rare
            Rarity.EPIC -> epic
            Rarity.LEGENDARY -> legendary
        }
        return list[variant.coerceIn(0, list.size - 1)]
    }
}

object Silhouettes {
    private val common = listOf(
        Silhouette(0.098f, 0.026f, 0.150f, StripeStyle.SWOOSH, 2, false),
        Silhouette(0.116f, 0.020f, 0.140f, StripeStyle.BLADE, 2, false),
        Silhouette(0.132f, 0.034f, 0.132f, StripeStyle.CHEVRON, 3, false),
    )
    private val rare = listOf(
        Silhouette(0.126f, 0.030f, 0.136f, StripeStyle.DUAL, 3, false),
        Silhouette(0.142f, 0.024f, 0.126f, StripeStyle.WAVE, 3, false),
        Silhouette(0.150f, 0.040f, 0.118f, StripeStyle.SPLIT, 4, false),
    )
    private val epic = listOf(
        Silhouette(0.138f, 0.032f, 0.128f, StripeStyle.SWOOSH, 4, false),
        Silhouette(0.156f, 0.026f, 0.108f, StripeStyle.SPLIT, 4, true),
        Silhouette(0.168f, 0.044f, 0.100f, StripeStyle.CHEVRON, 5, true),
    )
    private val legendary = listOf(
        Silhouette(0.160f, 0.038f, 0.112f, StripeStyle.WAVE, 5, false),
        Silhouette(0.178f, 0.048f, 0.096f, StripeStyle.SPLIT, 6, true),
    )

    fun of(rarity: Rarity, variant: Int): Silhouette {
        val list = when (rarity) {
            Rarity.COMMON -> common
            Rarity.RARE -> rare
            Rarity.EPIC -> epic
            Rarity.LEGENDARY -> legendary
        }
        return list[variant.coerceIn(0, list.size - 1)]
    }
}

// ─────────────────────────────────────────────────────────────
// 스니커즈
// ─────────────────────────────────────────────────────────────

data class Sneaker(
    val id: Long,
    val faction: Faction,
    val rarity: Rarity,
    /** 등급 내 변형 인덱스 (0부터) */
    val variant: Int,
    val level: Int,
    val mintNumber: Int,
    /** 행운 — 민팅 시 상위 등급 확률 보정 (1.00 ~ 1.60) */
    val luck: Double,
    /** 착화감 — 에너지 소모 절감 (1.00 ~ 1.40) */
    val comfort: Double,
    val durability: Int,
    val equipped: Boolean,
    val acquiredAt: Long,
) {
    val silhouette: Silhouette get() = Silhouettes.of(rarity, variant)

    /** 모델명 — "Apex", "Dragon" */
    val variantName: String get() = VariantNames.of(rarity, variant)

    /** 전체 이름 — "Fire Apex". 알림·토스트처럼 Composable 밖에서 쓴다. */
    val displayName: String get() = "${faction.displayName} $variantName"

    /**
     * 적립 배율. 등급 +1%, 같은 등급 내 변형 +0.3%, 레벨 +0.5%.
     * 최고 조합(전설2 Lv.30)이라도 약 +18% 수준으로 과하지 않다.
     */
    val boostPercent: Double
        get() = rarity.boostPercent + variant * 0.3 + (level - 1).coerceAtLeast(0) * 0.5

    val earningMultiplier: Double get() = 1.0 + boostPercent / 100.0

    /** 착화감이 높을수록 에너지를 덜 쓴다 (최대 15% 절감) */
    val energyEfficiency: Double
        get() = 1.0 - ((comfort - 1.0) * 0.375).coerceIn(0.0, 0.15)

    val upgradeCost: Double get() = RewardEconomy.sneakerUpgradeCost(level, rarity)

    val canUpgrade: Boolean get() = level < rarity.maxLevel

    /** 도감 슬롯 식별자 */
    val slotKey: String get() = "${faction.id}:${rarity.id}:$variant"
}

/** 민팅기 */
object SneakerMint {

    private fun rollStat(random: Random, rarity: Rarity, span: Double): Double {
        // 상위 등급일수록 좋은 롤이 나오되, 차이는 완만하게
        val base = random.nextDouble() * span
        val bonus = rarity.ordinal * span * 0.12
        return 1.0 + base + bonus
    }

    fun mint(
        random: Random,
        mintNumber: Int,
        luck: Double = 0.0,
        forcedRarity: Rarity? = null,
        forcedFaction: Faction? = null,
    ): Sneaker {
        val rarity = forcedRarity ?: Rarity.roll(random, luck)
        val faction = forcedFaction ?: Faction.entries[random.nextInt(Faction.entries.size)]
        val variant = random.nextInt(rarity.variantCount)
        return Sneaker(
            id = 0,
            faction = faction,
            rarity = rarity,
            variant = variant,
            level = 1,
            mintNumber = mintNumber,
            luck = rollStat(random, rarity, 0.45),
            comfort = rollStat(random, rarity, 0.30),
            durability = 100,
            equipped = false,
            acquiredAt = System.currentTimeMillis(),
        )
    }

    /** 첫 실행 시 지급하는 스타터 — Wind 초급 */
    fun starter(): Sneaker = Sneaker(
        id = 0,
        faction = Faction.WIND,
        rarity = Rarity.COMMON,
        variant = 0,
        level = 1,
        mintNumber = 1,
        luck = 1.15,
        comfort = 1.12,
        durability = 100,
        equipped = true,
        acquiredAt = System.currentTimeMillis(),
    )
}
