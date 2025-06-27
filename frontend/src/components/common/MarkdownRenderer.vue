<template>
  <div class="markdown-renderer" v-html="renderedMarkdown"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css'; // Or any other theme you prefer

const props = defineProps<{
  content: string | null | undefined;
}>();

// Configure marked to use highlight.js
marked.setOptions({
  highlight: function(code: string, lang: string) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext';
    return hljs.highlight(code, { language }).value;
  },
  langPrefix: 'hljs language-', // Used to prepend language classes for theming
  pedantic: false,
  gfm: true,
  breaks: false,
  sanitize: false,
  smartypants: false,
  xhtml: false
} as any); // Bypass TypeScript type checking for this option

const renderedMarkdown = computed(() => {
  if (!props.content) {
    return '<p>暂无内容。</p>';
  }
  return marked.parse(props.content);
});
</script>

<style scoped>
.markdown-renderer {
  line-height: 1.6;
}

.markdown-renderer :deep(h1),
.markdown-renderer :deep(h2),
.markdown-renderer :deep(h3),
.markdown-renderer :deep(h4),
.markdown-renderer :deep(h5),
.markdown-renderer :deep(h6) {
  margin-top: 20px;
  margin-bottom: 10px;
  font-weight: 600;
}

.markdown-renderer :deep(p) {
  margin-bottom: 10px;
}

.markdown-renderer :deep(ul),
.markdown-renderer :deep(ol) {
  margin-bottom: 10px;
  padding-left: 20px;
}

.markdown-renderer :deep(pre) {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
  margin-bottom: 10px;
}

.markdown-renderer :deep(code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  background-color: rgba(27,31,35,0.05);
  border-radius: 3px;
  padding: 0.2em 0.4em;
  font-size: 85%;
}

.markdown-renderer :deep(pre code) {
  padding: 0;
  background-color: transparent;
  border-radius: 0;
}

.markdown-renderer :deep(blockquote) {
  padding: 0 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
  margin: 0 0 16px;
}

.markdown-renderer :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 16px;
}

.markdown-renderer :deep(th),
.markdown-renderer :deep(td) {
  border: 1px solid #dfe2e5;
  padding: 8px 12px;
}

.markdown-renderer :deep(th) {
  background-color: #f6f8fa;
  font-weight: 600;
}
</style> 