package com.giwa.strideup.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/** FAQ 항목 — 질문 · 답변 문자열 리소스 쌍 */
private val faqEntries = listOf(
    R.string.faq_q_earn to R.string.faq_a_earn,
    R.string.faq_q_energy to R.string.faq_a_energy,
    R.string.faq_q_rarity to R.string.faq_a_rarity,
    R.string.faq_q_withdraw to R.string.faq_a_withdraw,
)

@Composable
fun SupportScreen(onBack: () -> Unit = {}) {
    var expandedIndex by rememberSaveable { mutableStateOf(-1) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Text(
                    text = stringResource(R.string.settings_support),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Snow,
                )
            }
        }

        item { SectionHeader(title = stringResource(R.string.support_faq)) }

        itemsIndexed(faqEntries) { index, (questionRes, answerRes) ->
            FaqCard(
                questionRes = questionRes,
                answerRes = answerRes,
                expanded = expandedIndex == index,
                onToggle = { expandedIndex = if (expandedIndex == index) -1 else index },
            )
        }

        item { ContactCard() }
    }
}

/** 접이식 FAQ 카드 — 질문 행 + 회전 셰브론, 펼치면 헤어라인 아래 답변 */
@Composable
private fun FaqCard(
    questionRes: Int,
    answerRes: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "faqChevron",
    )
    GlowCard(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
        spacing = 11.dp,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .quietClickable(onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(questionRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = if (expanded) Volt else Slate,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronRotation),
            )
        }
        if (expanded) {
            HairlineDivider()
            Text(
                text = stringResource(answerRes),
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
            )
        }
    }
}

/** 문의 카드 */
@Composable
private fun ContactCard() {
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            IconSquare(icon = Icons.Filled.SupportAgent, size = 38.dp)
            Text(
                text = stringResource(R.string.support_contact),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
        }
        Text(
            text = stringResource(R.string.support_contact_body),
            style = MaterialTheme.typography.bodySmall,
            color = Silver,
        )
    }
}
