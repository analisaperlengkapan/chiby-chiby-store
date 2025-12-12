package com.chibychibystore.data.local.database

import androidx.room.TypeConverter
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Role
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromRole(value: String?): Role? {
        return value?.let { Role.valueOf(it) }
    }

    @TypeConverter
    fun roleToString(role: Role?): String? {
        return role?.name
    }

    @TypeConverter
    fun fromPaymentMethod(value: String?): PaymentMethod? {
        return value?.let { PaymentMethod.valueOf(it) }
    }

    @TypeConverter
    fun paymentMethodToString(paymentMethod: PaymentMethod?): String? {
        return paymentMethod?.name
    }
}