#!/usr/bin/env node
/**
 * 扫描已发表文章，将正文和封面中的图片关联到 images 表
 */
const mysql = require('mysql2/promise');

(async () => {
  const conn = await mysql.createConnection({
    host: process.env.MYSQL_HOST || 'localhost',
    port: parseInt(process.env.MYSQL_PORT || '3306', 10),
    user: process.env.MYSQL_USER || 'root',
    password: process.env.MYSQL_PASS || 'root',
    database: process.env.MYSQL_DB || 'blog_db',
  });

  // 1. 获取所有已有图片记录
  const [existing] = await conn.query("SELECT `path` FROM images");
  const existingPaths = new Set(existing.map(r => r.path));

  // 2. 扫描 uploads 目录中的文件
  const scanDir = './uploads';
  const fs = require('fs');
  const pathMod = require('path');

  function walk(dir) {
    const files = [];
    if (!fs.existsSync(dir)) return files;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = pathMod.join(dir, entry.name);
      if (entry.isDirectory()) {
        files.push(...walk(full));
      } else if (/\.(png|jpg|jpeg|gif|webp|svg)$/i.test(entry.name)) {
        const relative = '/' + full.replace(/\\/g, '/');
        files.push({ path: relative, filename: entry.name, size: fs.statSync(full).size });
      }
    }
    return files;
  }

  const filesOnDisk = walk(scanDir);
  let inserted = 0;
  for (const f of filesOnDisk) {
    if (!existingPaths.has(f.path)) {
      await conn.query(
        "INSERT INTO images (`path`, `filename`, `size`, `type`) VALUES (?, ?, ?, 'unknown')",
        [f.path, f.filename, f.size]
      );
      inserted++;
    }
  }
  console.log(`📸 已扫描 ${filesOnDisk.length} 个文件，新增 ${inserted} 条记录`);

  // 3. 关联文章封面图
  const [posts] = await conn.query("SELECT id, title, cover_image, content FROM posts WHERE status = 1");
  let linked = 0;
  for (const post of posts) {
    // 封面图（可能存了完整 URL，去掉 http://... 部分）
    let coverPath = post.cover_image;
    if (coverPath) {
      if (coverPath.startsWith('http')) {
        try { coverPath = new URL(coverPath).pathname; } catch {}
      }
      const [res] = await conn.query(
        "UPDATE images SET post_id = ?, `type` = 'cover' WHERE `path` = ? AND (post_id IS NULL OR post_id != ?)",
        [post.id, coverPath, post.id]
      );
      linked += res.affectedRows || 0;
    }
    // 正文中的图片（兼容完整 URL 和相对路径）
    if (post.content) {
      const content = post.content.replace(/https?:\/\/[^\/]+/g, '');
      const matches = content.match(/\/uploads\/[^\s)"']+/g);
      if (matches) {
        for (const imgPath of [...new Set(matches)]) {
          const [res] = await conn.query(
            "UPDATE images SET post_id = ?, `type` = 'content' WHERE `path` = ? AND (post_id IS NULL OR post_id != ?)",
            [post.id, imgPath, post.id]
          );
          linked += res.affectedRows || 0;
        }
      }
    }
  }
  console.log(`🔗 已关联 ${linked} 张图片到文章`);

  await conn.end();
  console.log('✅ 扫描完成');
})();
