package com.enurse.app.domain.usecase.auth

import com.enurse.app.data.remote.dto.response.AuthResponse
import com.enurse.app.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(username: String, displayName: String, email: String, password: String, role: String, code: String): Result<AuthResponse> =
        repo.register(username, displayName, email, password, role, code)
}
