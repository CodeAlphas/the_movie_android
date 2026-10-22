plugins {
    id("movie.jvm.library")
    id("movie.quality")
}

dependencies {
    // MovieRepository가 PagingData를 돌려주어 :domain을 의존하는 모듈도 이 타입이 필요하므로,
    // implementation 대신 api 적용
    api(libs.androidx.paging.common)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
