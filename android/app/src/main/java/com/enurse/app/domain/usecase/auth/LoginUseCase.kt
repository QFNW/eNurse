package com.enurse.app.domain.usecase.auth

import com.enurse.app.data.remote.dto.response.AuthResponse
import com.enurse.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(login: String, password: String): Result<AuthResponse> =
        repo.login(login, password)
}
