package com.codealphas.themovie.data.map

import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.models.AddressFromServer
import com.codealphas.themovie.models.AddressItem
import com.codealphas.themovie.models.PoiItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TheaterMapperTest {
    @Test
    fun `극장 응답이면 noor 좌표와 이어 붙인 주소를 쓰고 front 좌표는 버려야 한다`() {
        val theater = poiItem().toTheater()

        assertEquals(
            Theater(
                name = "CGV 강남",
                latitude = 37.5,
                longitude = 127.5,
                address = "서울 강남구 역삼동 123",
            ),
            theater,
        )
    }

    @Test
    fun `주소 응답이면 fullAddress만 매핑해야 한다`() {
        assertEquals(
            Address(fullAddress = "서울특별시 강남구 역삼동 123"),
            addressFromServer(fullAddress = "서울특별시 강남구 역삼동 123").toAddress(),
        )
    }

    @Test
    fun `fullAddress가 null이면 Address의 fullAddress도 null이어야 한다`() {
        assertEquals(Address(fullAddress = null), addressFromServer(fullAddress = null).toAddress())
    }
}

private fun poiItem(): PoiItem =
    PoiItem(
        id = "1",
        name = "CGV 강남",
        telNo = "02-000-0000",
        frontLat = 36.0,
        frontLon = 126.0,
        noorLat = 37.5,
        noorLon = 127.5,
        upperAddrName = "서울",
        middleAddrName = "강남구",
        lowerAddrName = "역삼동",
        detailAddrName = "123",
        mlClass = "1",
        firstNo = "1",
        secondNo = "2",
        roadName = "테헤란로",
        radius = "7",
        rpFlag = "1",
        parkFlag = "0",
    )

private fun addressFromServer(fullAddress: String?): AddressFromServer =
    AddressFromServer(
        addressInfo =
            AddressItem(
                fullAddress = fullAddress,
                addressType = null,
                cityDo = "경기도",
                guGun = null,
                eupMyun = null,
                adminDong = null,
                adminDongCode = null,
                legalDong = null,
                legalDongCode = null,
                ri = null,
                roadName = null,
                buildingIndex = null,
                buildingName = null,
                mappingDistance = null,
                roadCode = null,
                bunji = null,
            ),
    )
