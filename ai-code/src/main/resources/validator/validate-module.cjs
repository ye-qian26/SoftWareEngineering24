const fs = require('node:fs')
const path = require('node:path')

function fail(message) {
  console.error(message)
  process.exit(1)
}

const projectRoot = process.argv[2]
const filePath = process.argv[3]

if (!projectRoot || !filePath) {
  fail('Usage: node validate-module.cjs <projectRoot> <filePath>')
}

const extension = path.extname(filePath).toLowerCase()
const source = fs.readFileSync(filePath, 'utf8')

try {
  if (
    extension === '.js' ||
    extension === '.mjs' ||
    extension === '.cjs' ||
    extension === '.ts' ||
    extension === '.tsx' ||
    extension === '.jsx'
  ) {
    let babelParser
    try {
      const parserPath = require.resolve('@babel/parser', { paths: [projectRoot] })
      babelParser = require(parserPath)
    } catch (error) {
      fail(`Cannot resolve @babel/parser from project root: ${error.message}`)
    }

    const plugins = []
    if (extension === '.jsx') {
      plugins.push('jsx')
    }
    if (extension === '.ts') {
      plugins.push('typescript')
    }
    if (extension === '.tsx') {
      plugins.push('typescript', 'jsx')
    }

    babelParser.parse(source, {
      sourceType: extension === '.cjs' ? 'script' : 'module',
      plugins
    })
    console.log(`Syntax valid: ${path.basename(filePath)}`)
    process.exit(0)
  }

  if (extension === '.css') {
    let postcss
    try {
      const postcssPath = require.resolve('postcss', { paths: [projectRoot] })
      postcss = require(postcssPath)
    } catch (error) {
      fail(`Cannot resolve postcss from project root: ${error.message}`)
    }

    postcss.parse(source, {
      from: filePath
    })
    console.log(`Syntax valid: ${path.basename(filePath)}`)
    process.exit(0)
  }

  console.log(`Validation skipped for ${path.basename(filePath)}`)
} catch (error) {
  fail(error.message || String(error))
}
