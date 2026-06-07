-- ============================================================
-- 为现有菜单项补充图标
-- ============================================================

-- 博客组
UPDATE menu_item SET icon = 'Reading'       WHERE id = 2;
UPDATE menu_item SET icon = 'Document'      WHERE id = 3;
UPDATE menu_item SET icon = 'EditPen'       WHERE id = 4;
UPDATE menu_item SET icon = 'CollectionTag' WHERE id = 5;
UPDATE menu_item SET icon = 'Files'         WHERE id = 6;

-- 个人组
UPDATE menu_item SET icon = 'Star'          WHERE id = 7;
UPDATE menu_item SET icon = 'Medal'         WHERE id = 8;

-- 发现组
UPDATE menu_item SET icon = 'Promotion'     WHERE id = 9;
UPDATE menu_item SET icon = 'Camera'        WHERE id = 10;

-- 工具组
UPDATE menu_item SET icon = 'ChatDotRound'  WHERE id = 11;
UPDATE menu_item SET icon = 'Monitor'       WHERE id = 12;

-- 管理后台组
UPDATE menu_item SET icon = 'DataLine'      WHERE id = 13;
UPDATE menu_item SET icon = 'UserFilled'    WHERE id = 14;
UPDATE menu_item SET icon = 'Comment'       WHERE id = 15;
UPDATE menu_item SET icon = 'Picture'       WHERE id = 16;
UPDATE menu_item SET icon = 'Operation'     WHERE id = 17;
