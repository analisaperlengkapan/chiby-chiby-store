package com.chibychibystore.data.local.database

import androidx.room.TypeConverter
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.local.entity.PromotionType
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

    @TypeConverter
    fun fromPromotionType(value: String?): PromotionType? {
        return value?.let { PromotionType.valueOf(it) }
    }

    @TypeConverter
    fun promotionTypeToString(type: PromotionType?): String? {
        return type?.name
    }

    @TypeConverter
    fun fromKategoriPengeluaran(value: String?): KategoriPengeluaran? {
        return value?.let { KategoriPengeluaran.valueOf(it) }
    }

    @TypeConverter
    fun kategoriPengeluaranToString(category: KategoriPengeluaran?): String? {
        return category?.name
    }
}