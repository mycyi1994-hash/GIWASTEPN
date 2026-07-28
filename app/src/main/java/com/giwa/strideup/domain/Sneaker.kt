package com.giwa.strideup.domain

import kotlin.math.pow
import kotlin.random.Random

/**
 * StrideUp 스니커즈 NFT.
 *
 * 수집욕은 세 축의 곱에서 나온다: **모델(실루엣) × 컬러웨이 × 희귀도**.
 * 같은 모델이라도 컬러웨이가 다르고, 같은 컬러웨이라도 희귀도에 따라
 * 스탯 범위와 발광 강도가 다르다. 여기에 민팅 번호(#0001)가 붙어
 * "내 것"이라는 감각을 만든다.
 */

// ─────────────────────────────────────────────────────────────
// 희귀도
// ─────────────────────────────────────────────────────────────

enum class Rarity(
    val id: String,
    /** 스탯 상한 배수 — 높을수록 좋은 스탯이 나온다 */
    val statScale: Double,
    /** 민팅 확률 가중치 */
    val weight: Int,
    /** 최대 강화 레벨 */
    val maxLevel: Int,
) {
    COMMON("COMMON", 1.00, 50, 10),
    UNCOMMON("UNCOMMON", 1.18, 27, 15),
    RARE("RARE", 1.40, 15, 20),
    EPIC("EPIC", 1.70, 6, 25),
    LEGENDARY("LEGENDARY", 2.10, 2, 30);

    companion object {
        fun of(id: String): Rarity = entries.firstOrNull { it.id == id } ?: COMMON

        /** 가중 추첨. luck(0.0~) 이 높을수록 상위 희귀도 가중치가 커진다. */
        fun roll(random: Random, luck: Double = 0.0): Rarity {
            val weights = entries.map { r ->
                // luck은 상위 등급일수록 크게 작용한다
                val boost = 1.0 + luck * 0.12 * r.ordinal
                r to (r.weight * boost)
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

// ─────────────────────────────────────────────────────────────
// 모델 (실루엣)
// ─────────────────────────────────────────────────────────────

/**
 * 실루엣 파라미터. 그리기 함수가 이 값들로 형태를 바꾸므로
 * 모델마다 눈에 띄게 다른 신발이 나온다.
 */
enum class SneakerModel(
    val id: String,
    val displayName: String,
    /** 미드솔 두께 (0.06 ~ 0.16) */
    val soleThickness: Float,
    /** 토박스 들림 (0.0 ~ 0.06) */
    val toeRise: Float,
    /** 발목 칼라 높이 (0.16 낮음 ~ 0.10 높음) */
    val collarTop: Float,
    /** 사이드 스트라이프 스타일 */
    val stripe: StripeStyle,
    /** 기본 스탯 성향 (효율/행운/착화감 가중) */
    val bias: Triple<Double, Double, Double>,
) {
    APEX_RUNNER(
        "APEX_RUNNER", "Apex Runner",
        soleThickness = 0.105f, toeRise = 0.030f, collarTop = 0.145f,
        stripe = StripeStyle.SWOOSH, bias = Triple(1.15, 0.95, 0.95),
    ),
    VOLT_TRAINER(
        "VOLT_TRAINER", "Volt Trainer",
        soleThickness = 0.135f, toeRise = 0.018f, collarTop = 0.125f,
        stripe = StripeStyle.BLADE, bias = Triple(0.95, 1.00, 1.20),
    ),
    TRAIL_BLAZER(
        "TRAIL_BLAZER", "Trail Blazer",
        soleThickness = 0.150f, toeRise = 0.048f, collarTop = 0.110f,
        stripe = StripeStyle.CHEVRON, bias = Triple(1.05, 1.15, 0.90),
    ),
    SPRINT_X(
        "SPRINT_X", "Sprint X",
        soleThickness = 0.078f, toeRise = 0.040f, collarTop = 0.160f,
        stripe = StripeStyle.DUAL, bias = Triple(1.25, 0.90, 0.90),
    ),
    FLOW_GLIDE(
        "FLOW_GLIDE", "Flow Glide",
        soleThickness = 0.120f, toeRise = 0.024f, collarTop = 0.135f,
        stripe = StripeStyle.WAVE, bias = Triple(1.00, 1.20, 1.05),
    );

    companion object {
        fun of(id: String): SneakerModel = entries.firstOrNull { it.id == id } ?: APEX_RUNNER
    }
}

enum class StripeStyle { SWOOSH, BLADE, CHEVRON, DUAL, WAVE }

// ─────────────────────────────────────────────────────────────
// 컬러웨이
// ─────────────────────────────────────────────────────────────

/**
 * ARGB 정수로 보관한다(Compose Color 의존성 없이 도메인에 두기 위해).
 * 전부 다크 베이스 + 네온 액센트 — 앱의 Volt 무드를 유지하면서 색만 갈린다.
 */
data class Colorway(
    val id: String,
    val displayName: String,
    val upper: Int,
    val upperShade: Int,
    val accent: Int,
    val accentSoft: Int,
    val sole: Int,
    /** 희귀도 하한 — 이 등급 이상에서만 등장한다 */
    val minRarity: Rarity = Rarity.COMMON,
)

object Colorways {
    val ALL = listOf(
        Colorway("VOLT_BLACK", "Volt Black", 0xFF16191C.toInt(), 0xFF0D0F11.toInt(), 0xFFC3FF3E.toInt(), 0xFFE4FF9F.toInt(), 0xFF23272B.toInt()),
        Colorway("CARBON", "Carbon Grey", 0xFF2A2E33.toInt(), 0xFF1B1F23.toInt(), 0xFFB6BDC4.toInt(), 0xFFE3E7EA.toInt(), 0xFF14171A.toInt()),
        Colorway("EMBER", "Ember", 0xFF1A1416.toInt(), 0xFF120E10.toInt(), 0xFFFF6B3D.toInt(), 0xFFFFB08A.toInt(), 0xFF2A1F22.toInt()),
        Colorway("ICE", "Ice Blue", 0xFF13191F.toInt(), 0xFF0C1116.toInt(), 0xFF4FD8FF.toInt(), 0xFFB4EEFF.toInt(), 0xFF1E262E.toInt()),
        Colorway("VIOLET", "Violet Pulse", 0xFF171320.toInt(), 0xFF100D17.toInt(), 0xFFA97BFF.toInt(), 0xFFD8C2FF.toInt(), 0xFF221C2E.toInt(), Rarity.UNCOMMON),
        Colorway("TOXIC", "Toxic Mint", 0xFF101A18.toInt(), 0xFF0A1211.toInt(), 0xFF3DFFB0.toInt(), 0xFFA8FFDD.toInt(), 0xFF1A2724.toInt(), Rarity.UNCOMMON),
        Colorway("SOLAR", "Solar Flare", 0xFF1C1810.toInt(), 0xFF13100A.toInt(), 0xFFFFD23D.toInt(), 0xFFFFEBA3.toInt(), 0xFF2A2417.toInt(), Rarity.RARE),
        Colorway("CRIMSON", "Crimson Edge", 0xFF1B1013.toInt(), 0xFF120A0D.toInt(), 0xFFFF3D6E.toInt(), 0xFFFF9FB8.toInt(), 0xFF2A181D.toInt(), Rarity.RARE),
        Colorway("ABYSS", "Abyss Teal", 0xFF0C1A1C.toInt(), 0xFF071213.toInt(), 0xFF19E6D0.toInt(), 0xFF9CFFF5.toInt(), 0xFF12262A.toInt(), Rarity.EPIC),
        Colorway("PLASMA", "Plasma", 0xFF1A1024.toInt(), 0xFF120A19.toInt(), 0xFFFF4FE0.toInt(), 0xFFFFB3F3.toInt(), 0xFF281838.toInt(), Rarity.EPIC),
        Colorway("AURUM", "Aurum", 0xFF1A160D.toInt(), 0xFF110E07.toInt(), 0xFFE8C36A.toInt(), 0xFFFFF0C4.toInt(), 0xFF2B2413.toInt(), Rarity.LEGENDARY),
        Colorway("PRISM", "Prism", 0xFF14161F.toInt(), 0xFF0D0E14.toInt(), 0xFF7CF5FF.toInt(), 0xFFFFC2F0.toInt(), 0xFF1E2130.toInt(), Rarity.LEGENDARY),
    )

    fun of(id: String): Colorway = ALL.firstOrNull { it.id == id } ?: ALL.first()

    /** 해당 희귀도에서 뽑을 수 있는 컬러웨이 */
    fun available(rarity: Rarity): List<Colorway> =
        ALL.filter { it.minRarity.ordinal <= rarity.ordinal }
}

// ─────────────────────────────────────────────────────────────
// 스니커즈
// ─────────────────────────────────────────────────────────────

data class Sneaker(
    val id: Long,
    val model: SneakerModel,
    val colorway: Colorway,
    val rarity: Rarity,
    val level: Int,
    val mintNumber: Int,
    /** 적립 효율 (1.0 ~ ) */
    val efficiency: Double,
    /** 행운 — 상위 희귀도 민팅 확률 */
    val luck: Double,
    /** 착화감 — 에너지 소모 절감 */
    val comfort: Double,
    /** 내구도 0~100 */
    val durability: Int,
    val equipped: Boolean,
    val acquiredAt: Long,
) {
    /** 이 스니커즈를 신었을 때의 SUP 적립 배율 */
    val earningMultiplier: Double
        get() = 1.0 + (efficiency - 1.0) + 0.15 * (level - 1).coerceAtLeast(0)

    /** 착화감이 높을수록 에너지를 덜 쓴다 (최대 25% 절감) */
    val energyEfficiency: Double
        get() = 1.0 - ((comfort - 1.0) * 0.20).coerceIn(0.0, 0.25)

    /** 다음 레벨 강화 비용 */
    val upgradeCost: Double
        get() = RewardEconomy.sneakerUpgradeCost(level, rarity)

    val canUpgrade: Boolean get() = level < rarity.maxLevel

    val displayName: String get() = "${colorway.displayName} ${model.displayName}"
}

/** 민팅기 — 새 스니커즈의 스탯을 뽑는다. */
object SneakerMint {

    /** 스탯 1개를 희귀도·모델 성향에 맞춰 뽑는다 */
    private fun rollStat(random: Random, rarity: Rarity, bias: Double): Double {
        // 0.0~1.0 을 제곱해 낮은 값이 흔하게 나오도록(상위 롤이 귀하게)
        val roll = random.nextDouble().pow(1.4)
        return 1.0 + roll * 0.55 * rarity.statScale * bias
    }

    fun mint(
        random: Random,
        mintNumber: Int,
        luck: Double = 0.0,
        forcedRarity: Rarity? = null,
        forcedModel: SneakerModel? = null,
    ): Sneaker {
        val rarity = forcedRarity ?: Rarity.roll(random, luck)
        val model = forcedModel ?: SneakerModel.entries[random.nextInt(SneakerModel.entries.size)]
        val palette = Colorways.available(rarity)
        val colorway = palette[random.nextInt(palette.size)]
        val (be, bl, bc) = model.bias
        return Sneaker(
            id = 0,
            model = model,
            colorway = colorway,
            rarity = rarity,
            level = 1,
            mintNumber = mintNumber,
            efficiency = rollStat(random, rarity, be),
            luck = rollStat(random, rarity, bl),
            comfort = rollStat(random, rarity, bc),
            durability = 100,
            equipped = false,
            acquiredAt = System.currentTimeMillis(),
        )
    }

    /** 첫 실행 시 지급하는 스타터 — 항상 Volt Black Apex Runner (Common) */
    fun starter(): Sneaker = Sneaker(
        id = 0,
        model = SneakerModel.APEX_RUNNER,
        colorway = Colorways.of("VOLT_BLACK"),
        rarity = Rarity.COMMON,
        level = 1,
        mintNumber = 1,
        efficiency = 1.20,
        luck = 1.10,
        comfort = 1.15,
        durability = 100,
        equipped = true,
        acquiredAt = System.currentTimeMillis(),
    )
}
