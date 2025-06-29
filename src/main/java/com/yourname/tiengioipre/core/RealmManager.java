    // ... các hàm khác ...

    /**
     * Áp dụng TOÀN BỘ chỉ số cho người chơi, bao gồm cả Cảnh Giới và Con Đường Tu Luyện.
     * @param player Người chơi cần áp dụng stats.
     */
    public void applyAllStats(Player player) {
        removeRealmStats(player); // Luôn xóa stats cũ trước

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        // 1. Lấy chỉ số từ Cảnh Giới (Tu Vi/Bậc)
        TierData tier = getTierData(data.getRealmId(), data.getTierId());
        if (tier == null) return;
        
        Map<String, Double> totalBonuses = new HashMap<>(tier.statBonuses());
        
        // 2. Lấy chỉ số từ Con Đường Tu Luyện và CỘNG GỘP vào
        String pathId = data.getCultivationPath();
        if (pathId != null && !pathId.equals("none")) {
            ConfigurationSection pathSection = plugin.getConfig().getConfigurationSection("paths." + pathId + ".stats");
            if (pathSection != null) {
                for (String key : pathSection.getKeys(false)) {
                    totalBonuses.merge(key, pathSection.getDouble(key), Double::sum);
                }
            }
        }
        
        // 3. Áp dụng TỔNG chỉ số
        totalBonuses.forEach((stat, value) -> {
            if (value == 0) return;
            Attribute attribute = null;
            AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;

            switch (stat) {
                case "max-health-bonus": attribute = Attribute.GENERIC_MAX_HEALTH; break;
                case "walk-speed-bonus": attribute = Attribute.GENERIC_MOVEMENT_SPEED; break;
                // Sát thương từ vũ khí và tay không sẽ được xử lý riêng trong sự kiện đánh
                case "weapon-damage-bonus":
                case "hand-damage-bonus":
                case "weapon-damage-modifier":
                    return; // Bỏ qua, sẽ xử lý ở event khác
            }

            if (attribute != null) {
                AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "tiengioi.stat." + stat, value, operation);
                player.getAttribute(attribute).addModifier(modifier);
            }
        });
        
        // 4. Áp dụng hiệu ứng Potion
        if (!tier.potionEffects().isEmpty()) {
            player.addPotionEffects(tier.potionEffects());
        }
    }