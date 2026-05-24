#!/usr/bin/env node
/**
 * MySQL 执行助手 — 直接运行 SQL 脚本或单条 SQL
 *
 * 用法:
 *   node mysql.js "SELECT * FROM posts LIMIT 3"
 *   node mysql.js -f path/to/migration.sql
 *   node mysql.js "ALTER TABLE comments ADD COLUMN test INT"
 *
 * 连接信息从 application.yml 读取，也可通过环境变量覆盖:
 *   MYSQL_HOST, MYSQL_PORT, MYSQL_DB, MYSQL_USER, MYSQL_PASS
 */

const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

const HOST = process.env.MYSQL_HOST || 'localhost';
const PORT = parseInt(process.env.MYSQL_PORT || '3306', 10);
const DB = process.env.MYSQL_DB || 'blog_db';
const USER = process.env.MYSQL_USER || 'root';
const PASS = process.env.MYSQL_PASS || 'root';

async function run() {
  const args = process.argv.slice(2);
  if (args.length === 0) {
    console.error('用法: node mysql.js "SQL语句" 或 node mysql.js -f <文件.sql>');
    process.exit(1);
  }

  let sql;
  if (args[0] === '-f') {
    const filePath = path.resolve(args[1]);
    if (!fs.existsSync(filePath)) {
      console.error('文件不存在:', filePath);
      process.exit(1);
    }
    sql = fs.readFileSync(filePath, 'utf8');
    console.error(`📄 读取文件: ${filePath}`);
  } else {
    sql = args.join(' ');
  }

  const conn = await mysql.createConnection({
    host: HOST,
    port: PORT,
    user: USER,
    password: PASS,
    database: DB,
    multipleStatements: true,
  });

  try {
    console.error(`🔌 已连接 ${HOST}:${PORT}/${DB}`);
    const statements = sql
      .split(';')
      .map(s => s.trim())
      .filter(s => s.length > 0 && !s.startsWith('--') && !s.startsWith('#'));

    let affectedRows = 0;

    // 处理 DELIMITER 语句（存储过程）
    if (sql.includes('DELIMITER')) {
      // 去掉 DELIMITER 行，按 $$ 分隔存储过程定义，其余按 ;
      const noDelimiter = sql.replace(/^DELIMITER\s+\S+\s*$/gm, '').replace(/DELIMITER\s+;$/gm, '');
      const blocks = noDelimiter.split(/\$\$/).map(s => s.trim()).filter(s => s);
      for (const block of blocks) {
        const cleanBlock = block.replace(/^\s*;+\s*/gm, '').trim();
        if (!cleanBlock) continue;
        try {
          const [result] = await conn.query(cleanBlock);
          if (result && result.affectedRows !== undefined) {
            affectedRows += result.affectedRows;
          }
          console.log(JSON.stringify(result, null, 2));
        } catch (err) {
          console.error(`❌ 块执行失败 (前80字符): ${cleanBlock.substring(0, 80)}...`);
          console.error(`   错误: ${err.message}`);
        }
      }
    } else {
      for (const stmt of statements) {
        try {
          const [result] = await conn.query(stmt);
          if (result && result.affectedRows !== undefined) {
            affectedRows += result.affectedRows;
          }
          console.log(JSON.stringify(result, null, 2));
        } catch (err) {
          console.error(`❌ 语句执行失败: ${stmt.substring(0, 80)}...`);
          console.error(`   错误: ${err.message}`);
        }
      }
    }

    if (affectedRows > 0) {
      console.error(`✅ 影响 ${affectedRows} 行`);
    } else {
      console.error('✅ 执行完成');
    }
  } finally {
    await conn.end();
  }
}

run().catch(err => {
  console.error('❌ 连接失败:', err.message);
  process.exit(1);
});
