package com.codealphas.themovie.data.map

import com.codealphas.themovie.data.map.remote.CoordToAddressDto
import com.codealphas.themovie.data.map.remote.KakaoLocalService
import com.codealphas.themovie.data.map.remote.KeywordSearchDto
import com.codealphas.themovie.data.map.remote.KeywordSearchMetaDto
import com.codealphas.themovie.data.map.remote.PlaceDto
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TheaterRepositoryImplTest {
    @Test
    fun `첫 페이지가 마지막이면 한 번만 요청해야 한다`() =
        runTest {
            val service = FakeKakaoLocalService(pages = listOf(page("A", isEnd = true)))

            val result = TheaterRepositoryImpl(service).getNearbyTheaters(latitude = 37.5, longitude = 127.0)

            assertEquals(listOf(1), service.requestedPages)
            assertEquals(listOf("A"), result.theaterNames())
        }

    @Test
    fun `마지막 페이지가 나올 때까지 페이지를 이어서 합쳐야 한다`() =
        runTest {
            val service = FakeKakaoLocalService(pages = listOf(page("A"), page("B", isEnd = true), page("C")))

            val result = TheaterRepositoryImpl(service).getNearbyTheaters(latitude = 37.5, longitude = 127.0)

            assertEquals(listOf(1, 2), service.requestedPages)
            assertEquals(listOf("A", "B"), result.theaterNames())
        }

    @Test
    fun `마지막 페이지가 계속 나오지 않아도 3페이지까지만 요청해야 한다`() =
        runTest {
            val service = FakeKakaoLocalService(pages = listOf(page("A"), page("B"), page("C"), page("D")))

            val result = TheaterRepositoryImpl(service).getNearbyTheaters(latitude = 37.5, longitude = 127.0)

            assertEquals(listOf(1, 2, 3), service.requestedPages)
            assertEquals(listOf("A", "B", "C"), result.theaterNames())
        }

    @Test
    fun `중간 페이지 요청이 실패하면 전체를 실패로 반환해야 한다`() =
        runTest {
            val service = FakeKakaoLocalService(pages = listOf(page("A")), failFromPage = 2)

            val result = TheaterRepositoryImpl(service).getNearbyTheaters(latitude = 37.5, longitude = 127.0)

            assertTrue(result is Outcome.Failure)
        }

    @Test
    fun `주소 요청은 경도를 x, 위도를 y로 넘겨야 한다`() =
        runTest {
            val service = FakeKakaoLocalService(pages = emptyList())

            TheaterRepositoryImpl(service).getAddress(latitude = 37.1, longitude = 127.2)

            assertEquals(127.2 to 37.1, service.requestedAddressXy)
        }
}

private fun DataResult<List<Theater>>.theaterNames(): List<String> = (this as Outcome.Success).data.map { it.name }

private fun page(
    name: String,
    isEnd: Boolean = false,
): KeywordSearchDto =
    KeywordSearchDto(
        documents =
            listOf(
                PlaceDto(
                    id = name,
                    placeName = name,
                    addressName = "지번",
                    roadAddressName = "도로명",
                    x = "127.0",
                    y = "37.5",
                ),
            ),
        meta = KeywordSearchMetaDto(isEnd = isEnd),
    )

private class FakeKakaoLocalService(
    private val pages: List<KeywordSearchDto>,
    private val failFromPage: Int = Int.MAX_VALUE,
) : KakaoLocalService {
    val requestedPages = mutableListOf<Int>()
    var requestedAddressXy: Pair<Double, Double>? = null

    override suspend fun getAddress(
        longitude: Double,
        latitude: Double,
    ): CoordToAddressDto {
        requestedAddressXy = longitude to latitude
        return CoordToAddressDto(documents = emptyList())
    }

    override suspend fun searchTheaters(
        longitude: Double,
        latitude: Double,
        query: String,
        categoryGroupCode: String,
        radiusMeters: Int,
        sort: String,
        size: Int,
        page: Int,
    ): KeywordSearchDto {
        requestedPages += page
        if (page >= failFromPage) throw IOException()
        return pages[page - 1]
    }
}
