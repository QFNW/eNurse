package com.enurse.app.domain.usecase.auth

import com.enurse.app.data.remote.dto.response.AuthResponse
import com.enurse.app.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, role: String): Result<AuthResponse> =
        repo.register(email, password, role)
}
