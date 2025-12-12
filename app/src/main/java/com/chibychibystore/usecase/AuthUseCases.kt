package com.chibychibystore.usecase

import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.service.AuthService
import com.chibychibystore.data.model.Result
import javax.inject.Inject

/**
 * Use case untuk login
 */
class LoginUseCase @Inject constructor(
    private val authService: AuthService
) {

    /**
     * Execute login dengan username dan password
     */
    suspend operator fun invoke(username: String, password: String): Result<Pengguna> {
        return authService.login(username, password)
    }
}

/**
 * Use case untuk logout
 */
class LogoutUseCase @Inject constructor(
    private val authService: AuthService
) {

    /**
     * Execute logout
     */
    suspend operator fun invoke(): Result<Unit> {
        return authService.logout()
    }
}

/**
 * Use case untuk mengubah password
 */
class ChangePasswordUseCase @Inject constructor(
    private val authService: AuthService
) {

    /**
     * Execute change password
     */
    suspend operator fun invoke(oldPassword: String, newPassword: String): Result<Unit> {
        return authService.changePassword(oldPassword, newPassword)
    }
}

/**
 * Use case untuk mendapatkan current user
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authService: AuthService
) {

    /**
     * Execute get current user
     */
    suspend operator fun invoke(): Pengguna? {
        return authService.getCurrentUser()
    }
}

/**
 * Use case untuk check permission
 */
class CheckPermissionUseCase @Inject constructor(
    private val authService: AuthService
) {

    /**
     * Execute permission check
     */
    suspend operator fun invoke(permission: String): Boolean {
        return authService.hasPermission(permission)
    }
}

data class AuthUseCases(
    val login: LoginUseCase,
    val logout: LogoutUseCase,
    val changePassword: ChangePasswordUseCase,
    val getCurrentUser: GetCurrentUserUseCase,
    val checkPermission: CheckPermissionUseCase
)