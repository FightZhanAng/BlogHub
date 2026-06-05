import MarkdownIt from 'markdown-it'

// markdown-it 实例（延迟初始化）
let md = null
function getMd() {
  if (!md) {
    md = new MarkdownIt({
      html: true,
      linkify: true,
      typographer: true,
      highlight(str, lang) {
        const language = lang || ''
        const escaped = str
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
        return `<pre class="language-${language}"><code class="language-${language}">${escaped}</code></pre>`
      },
    })
  }
  return md
}

/**
 * useMarkdown composable — 供组件使用
 */
export function useMarkdown() {
  function renderMarkdown(text) {
    if (!text) return ''
    return getMd().render(text)
  }
  return { renderMarkdown }
}

/**
 * 简易 frontmatter 解析（不依赖 gray-matter，避免 eval 问题）
 */
export function parseFrontmatter(raw) {
  const result = { frontmatter: {}, content: raw }

  // 检查是否有 --- 包裹的 frontmatter
  const match = raw.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n/)
  if (!match) return result

  const fmRaw = match[1]
  const content = raw.slice(match[0].length)
  const frontmatter = {}

  // 解析 key: value 行
  for (const line of fmRaw.split('\n')) {
    const sep = line.indexOf(':')
    if (sep === -1) continue
    const key = line.slice(0, sep).trim()
    let val = line.slice(sep + 1).trim()

    // 去掉首尾引号
    if ((val.startsWith("'") && val.endsWith("'")) ||
        (val.startsWith('"') && val.endsWith('"'))) {
      val = val.slice(1, -1)
    }
    // 数组： [a, b, c]
    else if (val.startsWith('[') && val.endsWith(']')) {
      val = val.slice(1, -1).split(',').map(s => s.trim().replace(/^['"]|['"]$/g, ''))
    }

    frontmatter[key] = val
  }

  result.frontmatter = frontmatter
  result.content = content
  return result
}

/**
 * 将 Markdown 原始字符串解析为 HTML + frontmatter
 */
export function markdownToHtml(raw) {
  const { frontmatter, content } = parseFrontmatter(raw)
  const html = getMd().render(content)
  return { html, frontmatter }
}
