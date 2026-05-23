const fs = require('node:fs')
const path = require('node:path')

function fail(message) {
  console.error(message)
  process.exit(1)
}

function normalizeError(error) {
  if (!error) {
    return 'Unknown Vue SFC validation error'
  }
  if (typeof error === 'string') {
    return error
  }
  if (error.message) {
    return error.message
  }
  return JSON.stringify(error)
}

const projectRoot = process.argv[2]
const filePath = process.argv[3]

if (!projectRoot || !filePath) {
  fail('Usage: node validate-vue-sfc.cjs <projectRoot> <filePath>')
}

let compilerSfc
try {
  const compilerPath = require.resolve('@vue/compiler-sfc', { paths: [projectRoot] })
  compilerSfc = require(compilerPath)
} catch (error) {
  fail(`Cannot resolve @vue/compiler-sfc from project root: ${error.message}`)
}

try {
  const source = fs.readFileSync(filePath, 'utf8')
  const { descriptor, errors } = compilerSfc.parse(source, { filename: filePath })
  if (errors.length > 0) {
    fail(normalizeError(errors[0]))
  }

  if (descriptor.script || descriptor.scriptSetup) {
    compilerSfc.compileScript(descriptor, { id: filePath })
  }

  if (descriptor.template) {
    const result = compilerSfc.compileTemplate({
      id: filePath,
      source: descriptor.template.content,
      filename: filePath
    })
    if (result.errors && result.errors.length > 0) {
      fail(normalizeError(result.errors[0]))
    }
  }

  for (const style of descriptor.styles) {
    if (style.lang && style.lang !== 'css') {
      fail(`Unsupported style language "${style.lang}" in ${path.basename(filePath)}. Use plain CSS.`)
    }
    const styleResult = compilerSfc.compileStyle({
      id: filePath,
      filename: filePath,
      source: style.content,
      scoped: style.scoped
    })
    if (styleResult.errors && styleResult.errors.length > 0) {
      fail(normalizeError(styleResult.errors[0]))
    }
  }

  console.log(`Vue SFC syntax valid: ${path.basename(filePath)}`)
} catch (error) {
  fail(normalizeError(error))
}
