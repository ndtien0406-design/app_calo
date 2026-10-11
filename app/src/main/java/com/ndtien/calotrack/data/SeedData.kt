package com.ndtien.calotrack.data

object SeedData {

    fun getFoods(): List<Food> = listOf(
        // ===== Tinh bột =====
        Food(name = "Cơm trắng", caloriesPerServing = 200.0, servingUnit = "bát", category = "Tinh bột"),
        Food(name = "Xôi mặn", caloriesPerServing = 400.0, servingUnit = "gói", category = "Tinh bột"),
        Food(name = "Xôi xéo", caloriesPerServing = 380.0, servingUnit = "gói", category = "Tinh bột"),
        Food(name = "Khoai lang luộc", caloriesPerServing = 100.0, servingUnit = "củ", category = "Tinh bột"),
        Food(name = "Bắp luộc", caloriesPerServing = 100.0, servingUnit = "trái", category = "Tinh bột"),
        Food(name = "Mì tôm", caloriesPerServing = 350.0, servingUnit = "gói", category = "Tinh bột"),

        // ===== Món chính =====
        Food(name = "Phở bò", caloriesPerServing = 450.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Phở gà", caloriesPerServing = 420.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Bánh mì thịt", caloriesPerServing = 400.0, servingUnit = "ổ", category = "Món chính"),
        Food(name = "Bánh mì pate", caloriesPerServing = 350.0, servingUnit = "ổ", category = "Món chính"),
        Food(name = "Bánh mì chả cá", caloriesPerServing = 350.0, servingUnit = "ổ", category = "Món chính"),
        Food(name = "Bún chả", caloriesPerServing = 550.0, servingUnit = "suất", category = "Món chính"),
        Food(name = "Bún bò Huế", caloriesPerServing = 550.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Bún riêu", caloriesPerServing = 480.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Hủ tiếu", caloriesPerServing = 450.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Bún thịt nướng", caloriesPerServing = 520.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Bún đậu mắm tôm", caloriesPerServing = 600.0, servingUnit = "suất", category = "Món chính"),
        Food(name = "Bánh canh", caloriesPerServing = 430.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Miến gà", caloriesPerServing = 380.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Mì Quảng", caloriesPerServing = 500.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Cơm tấm sườn", caloriesPerServing = 650.0, servingUnit = "đĩa", category = "Món chính"),
        Food(name = "Cơm gà", caloriesPerServing = 600.0, servingUnit = "đĩa", category = "Món chính"),
        Food(name = "Cơm chiên dương châu", caloriesPerServing = 600.0, servingUnit = "đĩa", category = "Món chính"),
        Food(name = "Cháo gà", caloriesPerServing = 300.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Cháo lòng", caloriesPerServing = 350.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Cháo sườn", caloriesPerServing = 320.0, servingUnit = "tô", category = "Món chính"),
        Food(name = "Bánh mì trứng", caloriesPerServing = 380.0, servingUnit = "ổ", category = "Món chính"),
        Food(name = "Bánh cuốn", caloriesPerServing = 350.0, servingUnit = "đĩa", category = "Món chính"),
        Food(name = "Bánh xèo", caloriesPerServing = 250.0, servingUnit = "cái", category = "Món chính"),
        Food(name = "Bánh bao nhân thịt", caloriesPerServing = 250.0, servingUnit = "cái", category = "Món chính"),
        Food(name = "Bánh giò", caloriesPerServing = 200.0, servingUnit = "cái", category = "Món chính"),
        Food(name = "Gà rán", caloriesPerServing = 300.0, servingUnit = "miếng", category = "Món chính"),
        Food(name = "Hủ tiếu xào", caloriesPerServing = 550.0, servingUnit = "đĩa", category = "Món chính"),
        Food(name = "Mì xào giòn", caloriesPerServing = 500.0, servingUnit = "đĩa", category = "Món chính"),

        // ===== Chất đạm =====
        Food(name = "Trứng gà luộc", caloriesPerServing = 70.0, servingUnit = "quả", category = "Chất đạm"),
        Food(name = "Trứng ốp la", caloriesPerServing = 90.0, servingUnit = "quả", category = "Chất đạm"),
        Food(name = "Ức gà", caloriesPerServing = 165.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Thịt gà luộc", caloriesPerServing = 180.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Cánh gà chiên", caloriesPerServing = 200.0, servingUnit = "cái", category = "Chất đạm"),
        Food(name = "Thịt heo nạc", caloriesPerServing = 200.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Thịt ba chỉ", caloriesPerServing = 350.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Sườn heo", caloriesPerServing = 300.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Thịt bò", caloriesPerServing = 250.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Cá basa", caloriesPerServing = 150.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Cá thu", caloriesPerServing = 190.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Tôm", caloriesPerServing = 100.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Mực", caloriesPerServing = 90.0, servingUnit = "100g", category = "Chất đạm"),
        Food(name = "Đậu hũ chiên", caloriesPerServing = 80.0, servingUnit = "miếng", category = "Chất đạm"),
        Food(name = "Xúc xích", caloriesPerServing = 150.0, servingUnit = "cây", category = "Chất đạm"),
        Food(name = "Chả cá", caloriesPerServing = 120.0, servingUnit = "miếng", category = "Chất đạm"),

        // ===== Rau củ =====
        Food(name = "Rau muống luộc", caloriesPerServing = 50.0, servingUnit = "đĩa", category = "Rau củ"),
        Food(name = "Canh rau", caloriesPerServing = 60.0, servingUnit = "bát", category = "Rau củ"),
        Food(name = "Canh chua cá", caloriesPerServing = 150.0, servingUnit = "bát", category = "Rau củ"),
        Food(name = "Rau cải xào tỏi", caloriesPerServing = 90.0, servingUnit = "đĩa", category = "Rau củ"),
        Food(name = "Salad trộn", caloriesPerServing = 120.0, servingUnit = "đĩa", category = "Rau củ"),
        Food(name = "Canh bí đỏ", caloriesPerServing = 80.0, servingUnit = "bát", category = "Rau củ"),
        Food(name = "Su su luộc", caloriesPerServing = 40.0, servingUnit = "đĩa", category = "Rau củ"),

        // ===== Món ăn nhẹ =====
        Food(name = "Gỏi cuốn", caloriesPerServing = 60.0, servingUnit = "cuốn", category = "Món ăn nhẹ"),
        Food(name = "Bò bía", caloriesPerServing = 80.0, servingUnit = "cuốn", category = "Món ăn nhẹ"),
        Food(name = "Phở cuốn", caloriesPerServing = 70.0, servingUnit = "cuốn", category = "Món ăn nhẹ"),
        Food(name = "Nem rán", caloriesPerServing = 150.0, servingUnit = "cái", category = "Món ăn nhẹ"),
        Food(name = "Chả giò", caloriesPerServing = 150.0, servingUnit = "cái", category = "Món ăn nhẹ"),
        Food(name = "Bánh tráng trộn", caloriesPerServing = 300.0, servingUnit = "phần", category = "Món ăn nhẹ"),
        Food(name = "Bánh tráng nướng", caloriesPerServing = 180.0, servingUnit = "cái", category = "Món ăn nhẹ"),
        Food(name = "Khoai tây chiên", caloriesPerServing = 350.0, servingUnit = "phần vừa", category = "Món ăn nhẹ"),

        // ===== Trái cây =====
        Food(name = "Táo", caloriesPerServing = 80.0, servingUnit = "quả", category = "Trái cây"),
        Food(name = "Chuối", caloriesPerServing = 105.0, servingUnit = "quả", category = "Trái cây"),
        Food(name = "Cam", caloriesPerServing = 60.0, servingUnit = "quả", category = "Trái cây"),
        Food(name = "Xoài", caloriesPerServing = 120.0, servingUnit = "quả", category = "Trái cây"),
        Food(name = "Dưa hấu", caloriesPerServing = 50.0, servingUnit = "miếng", category = "Trái cây"),
        Food(name = "Dứa (thơm)", caloriesPerServing = 60.0, servingUnit = "miếng", category = "Trái cây"),
        Food(name = "Ổi", caloriesPerServing = 70.0, servingUnit = "quả", category = "Trái cây"),
        Food(name = "Thanh long", caloriesPerServing = 60.0, servingUnit = "quả", category = "Trái cây"),

        // ===== Đồ uống =====
        Food(name = "Sữa tươi", caloriesPerServing = 150.0, servingUnit = "ly 250ml", category = "Đồ uống"),
        Food(name = "Sữa đậu nành", caloriesPerServing = 120.0, servingUnit = "ly", category = "Đồ uống"),
        Food(name = "Cà phê sữa", caloriesPerServing = 120.0, servingUnit = "ly", category = "Đồ uống"),
        Food(name = "Cà phê đen", caloriesPerServing = 10.0, servingUnit = "ly", category = "Đồ uống"),
        Food(name = "Trà sữa", caloriesPerServing = 350.0, servingUnit = "ly", category = "Đồ uống"),
        Food(name = "Nước ngọt", caloriesPerServing = 140.0, servingUnit = "lon", category = "Đồ uống"),
        Food(name = "Nước cam ép", caloriesPerServing = 100.0, servingUnit = "ly", category = "Đồ uống"),
        Food(name = "Trà đá", caloriesPerServing = 5.0, servingUnit = "ly", category = "Đồ uống"),
        Food(name = "Sinh tố bơ", caloriesPerServing = 280.0, servingUnit = "ly", category = "Đồ uống"),
        Food(name = "Nước dừa", caloriesPerServing = 60.0, servingUnit = "trái", category = "Đồ uống"),
        Food(name = "Nước mía", caloriesPerServing = 150.0, servingUnit = "ly", category = "Đồ uống"),

        // ===== Bánh kẹo =====
        Food(name = "Bánh flan", caloriesPerServing = 150.0, servingUnit = "hộp", category = "Bánh kẹo"),
        Food(name = "Chè đậu đen", caloriesPerServing = 250.0, servingUnit = "ly", category = "Bánh kẹo"),
        Food(name = "Chè thái", caloriesPerServing = 300.0, servingUnit = "ly", category = "Bánh kẹo"),
        Food(name = "Kem", caloriesPerServing = 200.0, servingUnit = "ly", category = "Bánh kẹo"),
        Food(name = "Bánh quy", caloriesPerServing = 50.0, servingUnit = "cái", category = "Bánh kẹo")
    )
}
