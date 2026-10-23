package com.codealphas.themovie.data.review.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.codealphas.themovie.data.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ReviewDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.reviewDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `가운데 감상문을 수정하면 목록에서 그 감상문이 맨 위에 와야 한다`() =
        runTest {
            dao.insert(entity(id = "-a", updatedAt = 1_000L))
            dao.insert(entity(id = "-b", updatedAt = 2_000L))
            dao.insert(entity(id = "-c", updatedAt = 3_000L))

            dao.update(entity(id = "-b", updatedAt = 4_000L))

            assertEquals(listOf("-b", "-c", "-a"), dao.getAll().first().map { it.id })
        }

    @Test
    fun `updatedAt이 같으면 목록이 id 내림차순이어야 한다`() =
        runTest {
            dao.insert(entity(id = "-a", updatedAt = 1_000L))
            dao.insert(entity(id = "-c", updatedAt = 1_000L))
            dao.insert(entity(id = "-b", updatedAt = 1_000L))

            assertEquals(listOf("-c", "-b", "-a"), dao.getAll().first().map { it.id })
        }

    @Test
    fun `대기 중인 감상문을 조회하면 SYNCED를 빼고 updatedAt 오름차순으로 돌려줘야 한다`() =
        runTest {
            dao.insert(entity(id = "-delete", updatedAt = 3_000L, syncState = ReviewSyncState.PENDING_DELETE))
            dao.insert(entity(id = "-synced", updatedAt = 1_000L, syncState = ReviewSyncState.SYNCED))
            dao.insert(entity(id = "-upsert", updatedAt = 2_000L, syncState = ReviewSyncState.PENDING_UPSERT))

            assertEquals(listOf("-upsert", "-delete"), dao.getPending().map { it.id })
        }

    @Test
    fun `대기 중인 감상문의 updatedAt이 같으면 id 오름차순으로 돌려줘야 한다`() =
        runTest {
            dao.insert(entity(id = "-b", updatedAt = 1_000L, syncState = ReviewSyncState.PENDING_UPSERT))
            dao.insert(entity(id = "-a", updatedAt = 1_000L, syncState = ReviewSyncState.PENDING_UPSERT))

            assertEquals(listOf("-a", "-b"), dao.getPending().map { it.id })
        }

    @Test
    fun `올린 localVersion과 같은 PENDING_UPSERT 감상문은 SYNCED로 바꿔야 한다`() =
        runTest {
            dao.insert(entity(id = "-a", syncState = ReviewSyncState.PENDING_UPSERT, localVersion = 2))

            val changed = dao.markSynced(id = "-a", localVersion = 2)

            assertEquals(1, changed)
            assertEquals(ReviewSyncState.SYNCED, dao.getById("-a")?.syncState)
        }

    @Test
    fun `업로드하는 동안 다시 수정되어 localVersion이 달라지면 PENDING_UPSERT로 남겨야 한다`() =
        runTest {
            dao.insert(entity(id = "-a", syncState = ReviewSyncState.PENDING_UPSERT, localVersion = 3))

            val changed = dao.markSynced(id = "-a", localVersion = 2)

            assertEquals(0, changed)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.getById("-a")?.syncState)
        }

    @Test
    fun `업로드하는 동안 삭제 표시되면 localVersion이 같아도 PENDING_DELETE로 남겨야 한다`() =
        runTest {
            dao.insert(entity(id = "-a", syncState = ReviewSyncState.PENDING_DELETE, localVersion = 2))

            val changed = dao.markSynced(id = "-a", localVersion = 2)

            assertEquals(0, changed)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.getById("-a")?.syncState)
        }

    @Test
    fun `PENDING_DELETE 감상문은 서버 삭제 뒤 행을 지워야 한다`() =
        runTest {
            dao.insert(entity(id = "-a", syncState = ReviewSyncState.PENDING_DELETE))

            val deleted = dao.deleteIfPendingDelete("-a")

            assertEquals(1, deleted)
            assertNull(dao.getById("-a"))
        }

    @Test
    fun `SYNCED나 PENDING_UPSERT 감상문은 서버 삭제 뒤에도 행을 지우지 않아야 한다`() =
        runTest {
            dao.insert(entity(id = "-synced", syncState = ReviewSyncState.SYNCED))
            dao.insert(entity(id = "-upsert", syncState = ReviewSyncState.PENDING_UPSERT))

            val deleted = dao.deleteIfPendingDelete("-synced") + dao.deleteIfPendingDelete("-upsert")

            assertEquals(0, deleted)
            assertEquals(ReviewSyncState.SYNCED, dao.getById("-synced")?.syncState)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.getById("-upsert")?.syncState)
        }

    @Test
    fun `같은 id의 감상문을 다시 추가하면 Room에 있던 감상문을 그대로 두어야 한다`() =
        runTest {
            dao.insert(entity(id = "-a", title = "Room 제목"))

            dao.insert(entity(id = "-a", title = "서버 제목"))

            assertEquals("Room 제목", dao.getById("-a")?.title)
        }

    @Test
    fun `감상문을 모두 지우면 목록이 비어야 한다`() =
        runTest {
            dao.insert(entity(id = "-a"))
            dao.insert(entity(id = "-b", syncState = ReviewSyncState.PENDING_UPSERT))

            dao.deleteAll()

            assertEquals(emptyList<ReviewEntity>(), dao.getAll().first())
        }
}

private fun entity(
    id: String,
    title: String = "기생충",
    updatedAt: Long = 1_000L,
    syncState: ReviewSyncState = ReviewSyncState.SYNCED,
    localVersion: Int = 0,
): ReviewEntity =
    ReviewEntity(
        title = title,
        image = "",
        content = "잘 봤다",
        createdAt = 1_000L,
        updatedAt = updatedAt,
        rating = 9.0,
        storageFileName = "",
        syncState = syncState,
        localVersion = localVersion,
        id = id,
    )
