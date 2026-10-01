package com.codealphas.themovie.presentation.map

import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.domain.map.TheaterRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.result.RemoteError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TheaterMapViewModelTest {
    @Test
    fun `위치를 받으면 주소와 영화관을 한 번씩 요청하고 상태에 담아야 한다`() {
        val repository = FakeTheaterRepository()
        runMapTest(repository, FakeLocationProvider(listOf(SEOUL))) { viewModel ->
            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()

            assertEquals(listOf(SEOUL), repository.addressRequests)
            assertEquals(listOf(SEOUL), repository.theaterRequests)
            assertEquals(TheaterMapUiState(SEOUL, Address("서울"), listOf(THEATER)), viewModel.state.value)
        }
    }

    @Test
    fun `위치를 받지 못하면 요청 없이 위치 실패를 알려야 한다`() {
        val repository = FakeTheaterRepository()
        runMapTest(repository, FakeLocationProvider(listOf(null))) { viewModel ->
            val effects = collectEffects(viewModel)

            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()

            assertEquals(listOf<TheaterMapEffect>(TheaterMapEffect.ShowLocationFailed), effects)
            assertEquals(emptyList<LocationLatLng>(), repository.addressRequests)
            assertEquals(emptyList<LocationLatLng>(), repository.theaterRequests)
            assertNull(viewModel.state.value.currentLocation)
        }
    }

    @Test
    fun `주소 요청이 실패하면 오류를 알리고 영화관은 상태에 남아야 한다`() {
        val repository = FakeTheaterRepository(addressResult = Outcome.Failure(RemoteError.Network))
        runMapTest(repository, FakeLocationProvider(listOf(SEOUL))) { viewModel ->
            val effects = collectEffects(viewModel)

            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()

            assertEquals(
                listOf(TheaterMapEffect.ShowError(RemoteError.Network)),
                effects.filterIsInstance<TheaterMapEffect.ShowError>(),
            )
            assertNull(viewModel.state.value.address)
            assertEquals(listOf(THEATER), viewModel.state.value.theaters)
        }
    }

    @Test
    fun `화면이 멈춘 동안 위치를 받지 못하면 다시 구독할 때 위치 실패를 받아야 한다`() {
        runMapTest(FakeTheaterRepository(), FakeLocationProvider(listOf(null))) { viewModel ->
            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()
            val effects = collectEffects(viewModel)

            assertEquals(listOf<TheaterMapEffect>(TheaterMapEffect.ShowLocationFailed), effects)
        }
    }

    @Test
    fun `화면이 멈춘 동안 위치를 받고 주소 요청이 실패하면 다시 구독할 때 위치와 오류를 받아야 한다`() {
        val repository = FakeTheaterRepository(addressResult = Outcome.Failure(RemoteError.Network))
        runMapTest(repository, FakeLocationProvider(listOf(SEOUL))) { viewModel ->
            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()
            val effects = collectEffects(viewModel)

            assertEquals(
                listOf(TheaterMapEffect.MoveCamera(SEOUL), TheaterMapEffect.ShowError(RemoteError.Network)),
                effects,
            )
        }
    }

    @Test
    fun `앞 위치의 응답이 늦게 와도 새 위치의 상태를 덮지 않아야 한다`() {
        val slowTheaters = CompletableDeferred<DataResult<List<Theater>>>()
        val repository = FakeTheaterRepository(slowTheatersAt = SEOUL, slowTheaters = slowTheaters)
        runMapTest(repository, FakeLocationProvider(listOf(SEOUL, BUSAN))) { viewModel ->
            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()
            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()
            slowTheaters.complete(Outcome.Success(listOf(OLD_THEATER)))
            runCurrent()

            assertEquals(BUSAN, viewModel.state.value.currentLocation)
            assertEquals(listOf(THEATER), viewModel.state.value.theaters)
        }
    }

    @Test
    fun `위치 설정을 켜지 못하면 위치 실패를 알려야 한다`() {
        runMapTest(FakeTheaterRepository(), FakeLocationProvider(emptyList())) { viewModel ->
            val effects = collectEffects(viewModel)

            viewModel.onIntent(TheaterMapIntent.LocationUnavailable)
            runCurrent()

            assertEquals(listOf<TheaterMapEffect>(TheaterMapEffect.ShowLocationFailed), effects)
        }
    }

    @Test
    fun `영화관 마커를 누르면 그 영화관을 시트에 보여야 한다`() {
        runMapTest(FakeTheaterRepository(), FakeLocationProvider(listOf(SEOUL))) { viewModel ->
            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()

            viewModel.onIntent(TheaterMapIntent.TheaterClicked(THEATER.id))

            assertEquals(THEATER, viewModel.state.value.selectedTheater)
        }
    }

    @Test
    fun `영화관 시트를 닫으면 시트에 영화관을 보이지 않아야 한다`() {
        runMapTest(FakeTheaterRepository(), FakeLocationProvider(listOf(SEOUL))) { viewModel ->
            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()
            viewModel.onIntent(TheaterMapIntent.TheaterClicked(THEATER.id))

            viewModel.onIntent(TheaterMapIntent.TheaterSheetDismissed)

            assertNull(viewModel.state.value.selectedTheater)
        }
    }

    @Test
    fun `영화관 시트를 연 뒤 새 위치를 받으면 새 목록에 같은 영화관이 있어도 시트를 닫아야 한다`() {
        runMapTest(FakeTheaterRepository(), FakeLocationProvider(listOf(SEOUL, BUSAN))) { viewModel ->
            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()
            viewModel.onIntent(TheaterMapIntent.TheaterClicked(THEATER.id))

            viewModel.onIntent(TheaterMapIntent.LocationReady)
            runCurrent()

            // 새 목록에 같은 영화관이 없으면 고른 영화관을 비우지 않아도 시트가 닫히므로, 새 목록에 같은 영화관이 있는지 먼저 확인
            assertEquals(listOf(THEATER), viewModel.state.value.theaters)
            assertNull(viewModel.state.value.selectedTheater)
        }
    }
}

private val SEOUL = LocationLatLng(37.5, 127.0)
private val BUSAN = LocationLatLng(35.1, 129.0)
private val THEATER =
    Theater(id = "1", name = "영화관", latitude = 37.5, longitude = 127.0, address = "주소", distanceMeters = 100)
private val OLD_THEATER = THEATER.copy(name = "이전 영화관")

private fun TestScope.collectEffects(viewModel: TheaterMapViewModel): List<TheaterMapEffect> {
    val effects = mutableListOf<TheaterMapEffect>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effect.collect { effects += it }
    }
    return effects
}

private fun runMapTest(
    repository: FakeTheaterRepository,
    locationProvider: FakeLocationProvider,
    body: suspend TestScope.(TheaterMapViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel = TheaterMapViewModel(repository, locationProvider)
    try {
        body(viewModel)
    } finally {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }
}

private class FakeLocationProvider(
    private val locations: List<LocationLatLng?>,
) : CurrentLocationProvider {
    private var index = 0

    override suspend fun getCurrentLocation(): LocationLatLng? = locations[index++]
}

private class FakeTheaterRepository(
    private val addressResult: DataResult<Address> = Outcome.Success(Address("서울")),
    private val slowTheatersAt: LocationLatLng? = null,
    private val slowTheaters: CompletableDeferred<DataResult<List<Theater>>>? = null,
) : TheaterRepository {
    val addressRequests = mutableListOf<LocationLatLng>()
    val theaterRequests = mutableListOf<LocationLatLng>()

    override suspend fun getAddress(
        latitude: Double,
        longitude: Double,
    ): DataResult<Address> {
        addressRequests += LocationLatLng(latitude, longitude)
        return addressResult
    }

    override suspend fun getNearbyTheaters(
        latitude: Double,
        longitude: Double,
    ): DataResult<List<Theater>> {
        val location = LocationLatLng(latitude, longitude)
        theaterRequests += location
        if (location == slowTheatersAt && slowTheaters != null) return slowTheaters.await()
        return Outcome.Success(listOf(THEATER))
    }
}
