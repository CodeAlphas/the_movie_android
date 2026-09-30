package com.codealphas.themovie.map

import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.domain.map.TheaterRepository
import com.codealphas.themovie.domain.result.DataResult
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
class MapViewModelTest {
    @Test
    fun `위치를 받으면 주소와 영화관을 한 번씩 요청하고 상태에 담아야 한다`() {
        val repository = FakeTheaterRepository()
        runMapTest(repository, FakeLocationProvider(listOf(SEOUL))) { viewModel ->
            viewModel.loadCurrentLocationAndTheaters()
            runCurrent()

            assertEquals(listOf(SEOUL), repository.addressRequests)
            assertEquals(listOf(SEOUL), repository.theaterRequests)
            assertEquals(MapUiState(SEOUL, Address("서울"), listOf(THEATER)), viewModel.uiState.value)
        }
    }

    @Test
    fun `위치를 받지 못하면 요청 없이 위치 실패를 알려야 한다`() {
        val repository = FakeTheaterRepository()
        runMapTest(repository, FakeLocationProvider(listOf(null))) { viewModel ->
            var failedCount = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.locationFailed.collect { failedCount++ }
            }

            viewModel.loadCurrentLocationAndTheaters()
            runCurrent()

            assertEquals(1, failedCount)
            assertEquals(emptyList<LocationLatLng>(), repository.addressRequests)
            assertEquals(emptyList<LocationLatLng>(), repository.theaterRequests)
            assertNull(viewModel.uiState.value.currentLocation)
        }
    }

    @Test
    fun `주소 요청이 실패하면 오류를 알리고 영화관은 상태에 남아야 한다`() {
        val repository = FakeTheaterRepository(addressResult = DataResult.Failure(RemoteError.Network))
        runMapTest(repository, FakeLocationProvider(listOf(SEOUL))) { viewModel ->
            val errors = mutableListOf<RemoteError>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.remoteError.collect { errors += it }
            }

            viewModel.loadCurrentLocationAndTheaters()
            runCurrent()

            assertEquals(listOf<RemoteError>(RemoteError.Network), errors)
            assertNull(viewModel.uiState.value.address)
            assertEquals(listOf(THEATER), viewModel.uiState.value.theaters)
        }
    }

    @Test
    fun `앞 위치의 응답이 늦게 와도 새 위치의 상태를 덮지 않아야 한다`() {
        val slowTheaters = CompletableDeferred<DataResult<List<Theater>>>()
        val repository = FakeTheaterRepository(slowTheatersAt = SEOUL, slowTheaters = slowTheaters)
        runMapTest(repository, FakeLocationProvider(listOf(SEOUL, BUSAN))) { viewModel ->
            viewModel.loadCurrentLocationAndTheaters()
            runCurrent()
            viewModel.loadCurrentLocationAndTheaters()
            runCurrent()
            slowTheaters.complete(DataResult.Success(listOf(OLD_THEATER)))
            runCurrent()

            assertEquals(BUSAN, viewModel.uiState.value.currentLocation)
            assertEquals(listOf(THEATER), viewModel.uiState.value.theaters)
        }
    }
}

private val SEOUL = LocationLatLng(37.5, 127.0)
private val BUSAN = LocationLatLng(35.1, 129.0)
private val THEATER =
    Theater(id = "1", name = "영화관", latitude = 37.5, longitude = 127.0, address = "주소", distanceMeters = 100)
private val OLD_THEATER = THEATER.copy(name = "이전 영화관")

private fun runMapTest(
    repository: FakeTheaterRepository,
    locationProvider: FakeLocationProvider,
    body: suspend TestScope.(MapViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel = MapViewModel(repository, locationProvider)
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
    private val addressResult: DataResult<Address> = DataResult.Success(Address("서울")),
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
        return DataResult.Success(listOf(THEATER))
    }
}
