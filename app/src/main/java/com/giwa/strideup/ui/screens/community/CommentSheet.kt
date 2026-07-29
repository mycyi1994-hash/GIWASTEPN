package com.giwa.strideup.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giwa.strideup.R
import com.giwa.strideup.domain.Comment
import com.giwa.strideup.domain.CommentThread
import com.giwa.strideup.domain.Post
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/**
 * 열려 있는 댓글 창을 화면에 붙여 주는 호스트.
 * 게시판·크루 게시판이 같은 뷰모델을 쓰므로 두 화면에서 그대로 재사용한다.
 */
@Composable
fun CommentSheetHost(viewModel: CommunityViewModel) {
    val openId by viewModel.openCommentsFor.collectAsStateWithLifecycle()
    val posts by viewModel.allPosts.collectAsStateWithLifecycle()
    val id = openId ?: return
    val post = posts.firstOrNull { it.id == id } ?: return
    val stream = remember(id) { viewModel.commentThreads(id) }
    val threads by stream.collectAsStateWithLifecycle(emptyList())
    val author = stringResource(R.string.rank_me)

    CommentSheet(
        post = post,
        threads = threads,
        onSend = { body, parentId -> viewModel.sendComment(id, body, parentId, author) },
        onDeleteComment = viewModel::deleteComment,
        onDismiss = viewModel::closeComments,
    )
}

/**
 * 댓글 창 — 글 카드의 댓글 아이콘을 누르면 아래에서 올라온다.
 *
 * 답글은 부모 댓글 아래에 한 칸 들여 붙는다. 답글 버튼을 누르면
 * 입력창 위에 "○○님에게 답글" 배너가 붙고, 보내면 대댓글로 저장된다.
 */
@Composable
fun CommentSheet(
    post: Post,
    threads: List<CommentThread>,
    onSend: (body: String, parentId: Long) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Comment?>(null) }
    val total = threads.sumOf { it.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .quietClickable(onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    // 시트 본체 탭이 배경으로 새어 나가 창이 닫히지 않게 흡수한다
                    .quietClickable { }
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(Carbon)
                    .border(
                        1.dp,
                        Edge,
                        RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    )
                    .imePadding(),
            ) {
                // 손잡이
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Edge),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Volt,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.comments_title, total),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = Slate,
                        modifier = Modifier
                            .size(18.dp)
                            .quietClickable(onDismiss),
                    )
                }

                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )

                Spacer(Modifier.size(10.dp))

                if (threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.comments_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(threads, key = { it.comment.id }) { thread ->
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                CommentRow(
                                    comment = thread.comment,
                                    onReply = { replyTo = thread.comment },
                                    onDelete = { onDeleteComment(thread.comment.id) },
                                )
                                thread.replies.forEach { reply ->
                                    CommentRow(
                                        comment = reply,
                                        onReply = { replyTo = thread.comment },
                                        onDelete = { onDeleteComment(reply.id) },
                                        modifier = Modifier.padding(start = 30.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                replyTo?.let { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonHigh)
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = Volt,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = stringResource(R.string.comments_replying_to, target.author),
                            fontSize = 11.sp,
                            color = Silver,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.common_cancel),
                            tint = Slate,
                            modifier = Modifier
                                .size(14.dp)
                                .quietClickable { replyTo = null },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(CarbonHigh)
                            .border(1.dp, Edge, RoundedCornerShape(50))
                            .padding(horizontal = 15.dp, vertical = 11.dp),
                    ) {
                        if (input.isEmpty()) {
                            Text(
                                text = stringResource(
                                    if (replyTo != null) {
                                        R.string.comments_reply_hint
                                    } else {
                                        R.string.comments_hint
                                    }
                                ),
                                fontSize = 13.sp,
                                color = Slate,
                            )
                        }
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            textStyle = TextStyle(color = Snow, fontSize = 13.sp),
                            cursorBrush = SolidColor(Volt),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    val canSend = input.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (canSend) Volt else CarbonHigh)
                            .then(
                                if (canSend) {
                                    Modifier.quietClickable {
                                        onSend(input.trim(), replyTo?.id ?: 0L)
                                        input = ""
                                        replyTo = null
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.comments_send),
                            tint = if (canSend) Night else Slate,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (comment.isReply) 24.dp else 30.dp)
                .clip(CircleShape)
                .background(if (comment.mine) Volt.copy(alpha = 0.18f) else CarbonHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = comment.author.take(1).uppercase(),
                color = if (comment.mine) Volt else Silver,
                fontSize = if (comment.isReply) 10.sp else 11.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = comment.author,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (comment.mine) Volt else Snow,
                )
                Text(
                    text = relativeTime(comment.createdAt),
                    fontSize = 10.sp,
                    color = Slate,
                )
            }
            Text(
                text = comment.body,
                fontSize = 12.5.sp,
                color = Silver,
                lineHeight = 18.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!comment.isReply) {
                    Text(
                        text = stringResource(R.string.comments_reply),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate,
                        modifier = Modifier.quietClickable(onReply),
                    )
                }
                if (comment.mine) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.post_delete),
                        tint = Slate,
                        modifier = Modifier
                            .size(13.dp)
                            .quietClickable(onDelete),
                    )
                }
            }
        }
    }
}
