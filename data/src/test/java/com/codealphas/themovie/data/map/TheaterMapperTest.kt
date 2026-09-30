package com.codealphas.themovie.data.map

import com.codealphas.themovie.data.map.remote.AddressNameDto
import com.codealphas.themovie.data.map.remote.CoordAddressDocumentDto
import com.codealphas.themovie.data.map.remote.CoordToAddressDto
import com.codealphas.themovie.data.map.remote.PlaceDto
import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TheaterMapperTest {
    @Test
    fun `도로명 주소가 있으면 도로명 주소와 숫자로 바꾼 좌표와 거리를 써야 한다`() {
        val theater = place().toTheaterOrNull()

        assertEquals(
            Theater(
                id = "10811159",
                name = "CGV 강남",
                latitude = 37.5,
                longitude = 127.5,
                address = "서울 강남구 강남대로 438",
                distanceMeters = 430,
            ),
            theater,
        )
    }

    @Test
    fun `도로명 주소가 비어 있으면 지번 주소를 써야 한다`() {
        assertEquals("서울 강남구 역삼동 814-6", place(roadAddressName = "").toTheaterOrNull()?.address)
    }

    @Test
    fun `거리가 비어 있으면 거리를 null로 써야 한다`() {
        assertNull(place(distance = "").toTheaterOrNull()?.distanceMeters)
    }

    @Test
    fun `좌표가 숫자가 아니면 목록에서 제외해야 한다`() {
        val theaters = listOf(place(), place(x = "abc")).toTheaters()

        assertEquals(1, theaters.size)
    }

    @Test
    fun `도로명 주소가 있으면 도로명 주소를 fullAddress로 써야 한다`() {
        val dto = addressResponse(road = "서울 강남구 강남대로 438", jibun = "서울 강남구 역삼동 814-6")

        assertEquals(Address(fullAddress = "서울 강남구 강남대로 438"), dto.toAddress())
    }

    @Test
    fun `도로명 주소가 없으면 지번 주소를 fullAddress로 써야 한다`() {
        val dto = addressResponse(road = null, jibun = "서울 서초구 서초동 1373")

        assertEquals(Address(fullAddress = "서울 서초구 서초동 1373"), dto.toAddress())
    }

    @Test
    fun `결과가 없으면 fullAddress가 null이어야 한다`() {
        assertEquals(Address(fullAddress = null), CoordToAddressDto(documents = emptyList()).toAddress())
    }
}

private fun place(
    roadAddressName: String = "서울 강남구 강남대로 438",
    x: String = "127.5",
    distance: String = "430",
): PlaceDto =
    PlaceDto(
        id = "10811159",
        placeName = "CGV 강남",
        addressName = "서울 강남구 역삼동 814-6",
        roadAddressName = roadAddressName,
        x = x,
        y = "37.5",
        distance = distance,
    )

private fun addressResponse(
    road: String?,
    jibun: String?,
): CoordToAddressDto =
    CoordToAddressDto(
        documents =
            listOf(
                CoordAddressDocumentDto(
                    roadAddress = road?.let(::AddressNameDto),
                    address = jibun?.let(::AddressNameDto),
                ),
            ),
    )
