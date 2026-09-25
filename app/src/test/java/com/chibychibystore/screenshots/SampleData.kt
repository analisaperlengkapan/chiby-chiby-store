package com.chibychibystore.screenshots

import com.chibychibystore.data.local.entity.AuditStatus
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Pelanggan
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.local.entity.Shift
import com.chibychibystore.data.local.entity.ShiftStatus
import com.chibychibystore.data.local.entity.StokOpname
import java.util.Date

/** Deterministic sample data so screenshots show realistic, populated screens. */
object SampleData {

    private val now = Date(1_767_225_600_000L) // 2026-01-01
    private fun daysAgo(days: Int) = Date(now.time - days * 86_400_000L)

    val owner = Pengguna(
        id = 1,
        username = "owner",
        passwordHash = "hash",
        role = Role.OWNER,
        isActive = true,
        createdAt = daysAgo(120),
        updatedAt = daysAgo(2)
    )

    val users = listOf(
        owner,
        Pengguna(2, "manager", "hash", Role.MANAGER, createdAt = daysAgo(90), updatedAt = daysAgo(3)),
        Pengguna(3, "kasir.putri", "hash", Role.CASHIER, createdAt = daysAgo(60), updatedAt = daysAgo(1)),
        Pengguna(4, "gudang.budi", "hash", Role.WAREHOUSE, createdAt = daysAgo(45), updatedAt = daysAgo(4)),
        Pengguna(5, "kasir.andi", "hash", Role.CASHIER, isActive = false, createdAt = daysAgo(30), updatedAt = daysAgo(7))
    )

    val categories = listOf(
        Kategori(1, "Minuman", "Aneka minuman kemasan dan segar", daysAgo(120)),
        Kategori(2, "Makanan Ringan", "Snack dan camilan", daysAgo(120)),
        Kategori(3, "Sembako", "Kebutuhan pokok sehari-hari", daysAgo(100)),
        Kategori(4, "Perawatan Tubuh", "Sabun, sampo, dan lainnya", daysAgo(80))
    )

    val warehouses = listOf(
        Gudang(1, "Gudang Utama", "Lantai 1 - Toko Depan", 1200, daysAgo(120)),
        Gudang(2, "Gudang Belakang", "Lantai 2 - Belakang", 800, daysAgo(110))
    )

    val suppliers = listOf(
        Pemasok(1, "PT Sinar Minuman Nusantara", "0811-2345-6789", "Jl. Industri Raya No. 12, Jakarta", "sales@sinarminuman.co.id", daysAgo(100)),
        Pemasok(2, "CV Pangan Sejahtera", "0812-9876-5432", "Jl. Pasar Baru No. 45, Bandung", "order@pangansejahtera.id", daysAgo(95)),
        Pemasok(3, "UD Makmur Jaya", "0857-1122-3344", "Jl. Raya Solo KM 8, Yogyakarta", "makmurjaya@gmail.com", daysAgo(70))
    )

    val products = listOf(
        Produk(1, "Air Mineral 600ml", "8991001101010", 1, 2500.0, 4000.0, 240, 1, 40, null, daysAgo(100), daysAgo(3)),
        Produk(2, "Teh Kotak 250ml", "8991001101027", 1, 3200.0, 5000.0, 180, 1, 30, null, daysAgo(100), daysAgo(3)),
        Produk(3, "Kopi Sachet Robusta", "8991001101034", 1, 1200.0, 2000.0, 12, 1, 25, null, daysAgo(90), daysAgo(1)),
        Produk(4, "Keripik Kentang 68g", "8991001101041", 2, 6500.0, 9500.0, 95, 1, 20, null, daysAgo(80), daysAgo(5)),
        Produk(5, "Biskuit Cokelat 120g", "8991001101058", 2, 5200.0, 7800.0, 8, 1, 15, null, daysAgo(80), daysAgo(2)),
        Produk(6, "Beras Premium 5kg", "8991001101065", 3, 58000.0, 68000.0, 60, 1, 10, null, daysAgo(60), daysAgo(6)),
        Produk(7, "Minyak Goreng 2L", "8991001101072", 3, 28000.0, 34000.0, 45, 1, 12, null, daysAgo(60), daysAgo(6)),
        Produk(8, "Sabun Mandi Cair 450ml", "8991001101089", 4, 18000.0, 24500.0, 33, 2, 10, null, daysAgo(50), daysAgo(8))
    )

    val lowStock = products.filter { it.stockQuantity <= it.minStock }

    val customers = listOf(
        Pelanggan(1, "Dewi Lestari", "0813-5555-1111", "dewi@example.com", "Jl. Melati No. 3", 245, daysAgo(60), daysAgo(2)),
        Pelanggan(2, "Budi Santoso", "0813-5555-2222", "budi@example.com", "Jl. Kenanga No. 10", 120, daysAgo(45), daysAgo(4)),
        Pelanggan(3, "Rina Marlina", "0813-5555-3333", "rina@example.com", "Jl. Anggrek No. 7", 880, daysAgo(30), daysAgo(1))
    )

    val sales = listOf(
        sale(7, daysAgo(0), PaymentMethod.CASH, 184_500.0),
        sale(8, daysAgo(0), PaymentMethod.QRIS, 96_000.0),
        sale(9, daysAgo(1), PaymentMethod.CARD, 342_800.0),
        sale(10, daysAgo(1), PaymentMethod.CASH, 57_500.0),
        sale(11, daysAgo(2), PaymentMethod.QRIS, 219_000.0)
    )

    private fun sale(id: Long, date: Date, method: PaymentMethod, total: Double) = Penjualan(
        id = id,
        saleDate = date,
        totalAmount = total,
        tax = total * 0.11,
        discount = 5000.0,
        paymentMethod = method,
        cashierId = 3,
        shiftId = 1,
        pelangganId = 1,
        warehouseId = 1,
        createdAt = date,
        pointsEarned = (total / 10_000).toInt()
    )

    val saleItems = listOf(
        ItemPenjualan(1, 7, 1, 12, 4000.0, 48_000.0, 2500.0),
        ItemPenjualan(2, 7, 4, 8, 9500.0, 76_000.0, 6500.0),
        ItemPenjualan(3, 7, 6, 1, 68_000.0, 68_000.0, 58_000.0)
    )

    val saleWithItems = PenjualanWithItems(sales.first(), saleItems)

    val purchases = listOf(
        Pembelian(1, daysAgo(3), 1, 1, "INV-2026-0001", 4_850_000.0, "Restock minuman bulanan", 2, daysAgo(3), daysAgo(3)),
        Pembelian(2, daysAgo(5), 2, 1, "INV-2026-0002", 2_310_000.0, "Snack dan biskuit", 2, daysAgo(5), daysAgo(5)),
        Pembelian(3, daysAgo(9), 3, 2, "INV-2026-0003", 1_760_000.0, "Sembako", 4, daysAgo(9), daysAgo(9))
    )

    val expenses = listOf(
        Pengeluaran(1, daysAgo(1), KategoriPengeluaran.UTILITIES, 1_250_000.0, "Tagihan listrik dan air", 1, 1, daysAgo(1)),
        Pengeluaran(2, daysAgo(2), KategoriPengeluaran.SALARIES_WAGES, 8_400_000.0, "Gaji karyawan bulan ini", 1, 1, daysAgo(2)),
        Pengeluaran(3, daysAgo(4), KategoriPengeluaran.MARKETING_ADVERTISING, 750_000.0, "Iklan media sosial", 2, 1, daysAgo(4)),
        Pengeluaran(4, daysAgo(6), KategoriPengeluaran.SUPPLIES_MAINTENANCE, 320_000.0, "Perawatan peralatan kasir", 2, 1, daysAgo(6))
    )

    val promotions = listOf(
        Promotion(1, "Diskon Gajian", "Potongan 10% untuk transaksi di atas Rp 200.000", PromotionType.PERCENTAGE, 10.0, 200_000.0, 50_000.0, true, daysAgo(10), now, daysAgo(10), daysAgo(1)),
        Promotion(2, "Promo Sembako", "Potongan langsung Rp 15.000 untuk kategori sembako", PromotionType.FIXED_AMOUNT, 15_000.0, 150_000.0, null, true, daysAgo(20), now, daysAgo(20), daysAgo(1)),
        Promotion(3, "Flash Sale Akhir Pekan", "Diskon 20% khusus akhir pekan", PromotionType.PERCENTAGE, 20.0, 100_000.0, 100_000.0, false, daysAgo(30), daysAgo(20), daysAgo(30), daysAgo(20))
    )

    val shifts = listOf(
        Shift(1, 3, daysAgo(0), null, 500_000.0, 780_500.0, null, 280_500.0, 65_000.0, "Shift pagi", ShiftStatus.OPEN, daysAgo(0)),
        Shift(2, 5, daysAgo(1), daysAgo(1), 500_000.0, 1_120_000.0, 1_110_000.0, 620_000.0, 40_000.0, "Shift sore", ShiftStatus.CLOSED, daysAgo(1)),
        Shift(3, 3, daysAgo(2), daysAgo(2), 400_000.0, 890_000.0, 895_000.0, 490_000.0, 22_000.0, "Shift pagi", ShiftStatus.CLOSED, daysAgo(2))
    )

    val audits = listOf(
        StokOpname(1, daysAgo(3), 1, 2, "Opname rutin bulanan", AuditStatus.COMPLETED, daysAgo(3)),
        StokOpname(2, daysAgo(1), 2, 4, "Cek selisih stok gudang belakang", AuditStatus.DRAFT, daysAgo(1))
    )
}
