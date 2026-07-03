package com.enurse.app.viewmodel

/**
 * Unit tests for AuthViewModel.
 * Run: ./gradlew test --tests AuthViewModelTest
 *
 * Requires @RunWith(MockitoJUnitRunner::class) and mock dependencies when SDK is available.
 *
 * Test coverage:
 * - Login with valid credentials → success state with userRole
 * - Login with invalid credentials → error state
 * - Register → success state
 * - Register with duplicate email → error state
 * - Logout → clears token
 * - Loading state transitions
 */
class AuthViewModelTest {

    // When Android SDK is available, uncomment:
    /*
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var authRepository: AuthRepository

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = AuthViewModel(authRepository)
    }

    @Test
    fun `login with valid credentials emits success state`() = runTest {
        val response = AuthResponse(
            accessToken = "at", refreshToken = "rt",
            user = UserBrief(id = "1", email = "test@test.com", role = "PATIENT")
        )
        whenever(authRepository.login("test@test.com", "Test1234"))
            .thenReturn(Result.success(response))

        viewModel.login("test@test.com", "Test1234")

        advanceUntilIdle()
        val state = viewModel.state.first()
        assertTrue(state.success)
        assertEquals("PATIENT", state.userRole)
    }

    @Test
    fun `login with wrong password emits error state`() = runTest {
        whenever(authRepository.login("test@test.com", "wrong"))
            .thenReturn(Result.failure(Exception("邮箱或密码错误")))

        viewModel.login("test@test.com", "wrong")

        advanceUntilIdle()
        val state = viewModel.state.first()
        assertFalse(state.success)
        assertEquals("邮箱或密码错误", state.error)
    }

    @Test
    fun `register emits success state with correct role`() = runTest {
        val response = AuthResponse(
            accessToken = "at", refreshToken = "rt",
            user = UserBrief(id = "1", email = "test@test.com", role = "PATIENT")
        )
        whenever(authRepository.register("test@test.com", "Test1234", "PATIENT"))
            .thenReturn(Result.success(response))

        viewModel.register("test@test.com", "Test1234", "PATIENT")

        advanceUntilIdle()
        val state = viewModel.state.first()
        assertTrue(state.success)
        assertEquals("PATIENT", state.userRole) // use server-returned role
    }

    @Test
    fun `register duplicate email emits error`() = runTest {
        whenever(authRepository.register("dup@test.com", "Test1234", "PATIENT"))
            .thenReturn(Result.failure(Exception("该邮箱已被注册")))

        viewModel.register("dup@test.com", "Test1234", "PATIENT")

        advanceUntilIdle()
        val state = viewModel.state.first()
        assertFalse(state.success)
        assertEquals("该邮箱已被注册", state.error)
    }

    @Test
    fun `logout calls repository`() = runTest {
        viewModel.logout()
        advanceUntilIdle()
        verify(authRepository).logout()
    }

    @Test
    fun `login sets isLoading true during request`() = runTest {
        // Chain: start loading → on success → loading false
        val response = AuthResponse("at", "rt", UserBrief("1", "t", "PATIENT"))
        whenever(authRepository.login(any(), any())).thenReturn(Result.success(response))

        val states = mutableListOf<AuthUiState>()
        val job = launch {
            viewModel.state.collect { states.add(it) }
        }

        viewModel.login("t@t.com", "Test1234")
        advanceUntilIdle()

        // States: [initial, loading...]
        assertTrue(states.any { it.isLoading })
        assertTrue(states.lastOrNull()?.success == true)

        job.cancel()
    }
    */
}
