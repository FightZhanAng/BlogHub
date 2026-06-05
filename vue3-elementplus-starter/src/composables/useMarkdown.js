import MarkdownIt from 'markdown-it'

let md = null
function getMd() {
  if (!md) {
    md = new MarkdownIt({
      html: false,
      linkify: true,
      typographer: true,
      breaks: true,
      highlight(str, lang) {
        const language = lang || 'code'
        const escaped = str
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
        const lineCount = str.split('\n').length
        const collapsed = lineCount > 15 ? ' collapsed' : ''
        const collapseHint = lineCount > 15 ? `<span class="code-line-count">${lineCount} 行</span>` : ''
        return `<div class="code-block${collapsed}" data-lang="${language}"><div class="code-header"><span class="code-lang">${language}</span>${collapseHint}<div class="code-actions"><button class="code-copy-btn" onclick="window.__copyCode(this)" title="复制"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg> 复制</button><button class="code-collapse-btn" onclick="window.__toggleCode(this)" title="折叠"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg></button></div></div><pre class="language-${language}"><code class="language-${language}">${escaped}</code></pre></div>`
      },
    })
  }
  return md
}

// 全局复制代码函数
if (typeof window !== 'undefined' && !window.__copyCode) {
  window.__copyCode = function(btn) {
    const codeBlock = btn.closest('.code-block')
    const code = codeBlock.querySelector('code')
    const text = code.textContent
    navigator.clipboard.writeText(text).then(() => {
      btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg> 已复制'
      setTimeout(() => {
        btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg> 复制'
      }, 2000)
    })
  }
  window.__toggleCode = function(btn) {
    const codeBlock = btn.closest('.code-block')
    const pre = codeBlock.querySelector('pre')
    const isCollapsed = codeBlock.classList.contains('collapsed')
    if (isCollapsed) {
      codeBlock.classList.remove('collapsed')
      pre.style.display = ''
      btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>'
    } else {
      codeBlock.classList.add('collapsed')
      btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>'
    }
  }
}

/**
 * 预处理 AI 回复内容，修复常见 markdown 格式问题
 */
function preprocessContent(text) {
  if (!text) return ''
  let result = text

  // 1. 修复：把 >开头的行（> 用于列表项）从 blockquote 格式转为列表
  //    AI 经常用 > 代替 -
  result = result.replace(/^>\s*(\d+\.)\s/gm, '$1 ')

  // 2. 修复：确保代码块前后有空行
  result = result.replace(/```/g, '\n```\n')
  // 清理连续空行
  result = result.replace(/\n{3,}/g, '\n\n')

  return result
}

/**
 * useMarkdown composable
 */
export function useMarkdown() {
  function renderMarkdown(text) {
    if (!text) return ''
    const preprocessed = preprocessContent(text)
    return getMd().render(preprocessed)
  }
  return { renderMarkdown }
}

/**
 * 简易 frontmatter 解析
 */
export function parseFrontmatter(raw) {
  const result = { frontmatter: {}, content: raw }
  const match = raw.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n/)
  if (!match) return result

  const fmRaw = match[1]
  const content = raw.slice(match[0].length)
  const frontmatter = {}

  for (const line of fmRaw.split('\n')) {
    const sep = line.indexOf(':')
    if (sep === -1) continue
    const key = line.slice(0, sep).trim()
    let val = line.slice(sep + 1).trim()
    if ((val.startsWith("'") && val.endsWith("'")) ||
        (val.startsWith('"') && val.endsWith('"'))) {
      val = val.slice(1, -1)
    } else if (val.startsWith('[') && val.endsWith(']')) {
      val = val.slice(1, -1).split(',').map(s => s.trim().replace(/^['"]|['"]$/g, ''))
    }
    frontmatter[key] = val
  }

  result.frontmatter = frontmatter
  result.content = content
  return result
}

export function markdownToHtml(raw) {
  const { frontmatter, content } = parseFrontmatter(raw)
  const html = getMd().render(content)
  return { html, frontmatter }
}
