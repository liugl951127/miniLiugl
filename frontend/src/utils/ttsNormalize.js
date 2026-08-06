/**
 * V5.4+ TTS 文本规范化
 * 
 * 在传给 speechSynthesis.speak() 之前调用, 把数字/金额/日期/时间等
 * 转换为自然的中文/英文读法, 避免 "1234.56" 读成 "one two three four dot five six"
 * 
 * 规则 (按顺序):
 * 1. URL / 邮箱 → 字母/单词化
 * 2. 金额: ¥1,234.56 → "1234 元 5 角 6 分"
 * 3. 百分比: 50% → "百分之五十"
 * 4. 日期: 2026-08-03 → "2026 年 8 月 3 日"
 * 5. 时间: 14:30 → "14 点 30 分"
 * 6. 手机号: 18812345678 → "188-1234-5678" (1-8-8-1...)
 * 7. 大数字: 1234567 → "123 万 4567" / "1234567"
 * 8. 小数: 3.14 → "3 点 1 4"
 * 9. 单位: kg/m/cm/GB/MB/KB/m²/m³/℃/° → 字母/单词
 */

const UNITS = {
  // 长度
  'mm': '毫米', 'cm': '厘米', 'm': '米', 'km': '千米',
  'μm': '微米', 'nm': '纳米',
  // 重量
  'mg': '毫克', 'g': '克', 'kg': '千克', 't': '吨',
  // 面积/体积
  'm²': '平方米', 'm³': '立方米', 'cm²': '平方厘米', 'cm³': '立方厘米',
  'km²': '平方千米', 'km³': '立方千米',
  // 时间
  'ms': '毫秒', 's': '秒', 'min': '分钟', 'h': '小时', 'd': '天', 'w': '周',
  // 数据
  'B': '字节', 'KB': '千字节', 'MB': '兆字节', 'GB': '吉字节', 'TB': '太字节',
  'bps': '比特每秒', 'kbps': '千比特每秒', 'Mbps': '兆比特每秒', 'Gbps': '吉比特每秒',
  // 温度
  '℃': '摄氏度', '°C': '摄氏度', '°F': '华氏度', 'K': '开尔文',
  // 频率
  'Hz': '赫兹', 'kHz': '千赫兹', 'MHz': '兆赫兹', 'GHz': '吉赫兹',
  // 压强
  'Pa': '帕斯卡', 'kPa': '千帕', 'MPa': '兆帕',
  // 电压
  'V': '伏特', 'mV': '毫伏', 'kV': '千伏',
  // 电流
  'A': '安培', 'mA': '毫安',
  // 功率
  'W': '瓦特', 'kW': '千瓦', 'MW': '兆瓦',
}

/**
 * 数字转中文 (整数部分)
 * - 1234 → 一千二百三十四
 * - 1000000 → 一百万
 * - 10005 → 一万零五
 * - 100000000 → 一亿
 */
function intToZh(num) {
  if (num === 0) return '零'
  if (num < 0) return '负' + intToZh(-num)
  
  const digits = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九']
  
  if (num < 10) return digits[num]
  if (num < 20) return num === 10 ? '十' : '十' + digits[num - 10]
  
  // 分段 (每 4 位一段)
  const parts = []
  let n = num
  while (n > 0) {
    parts.unshift(n % 10000)
    n = Math.floor(n / 10000)
  }
  
  const bigUnits = ['', '万', '亿', '万亿']
  
  // 处理单段 (0-9999) → 中文
  function partToZh(p) {
    if (p === 0) return ''
    if (p < 10) return digits[p]
    if (p < 20) return '十' + (p === 10 ? '' : digits[p - 10])
    
    const thousands = Math.floor(p / 1000)
    const hundreds = Math.floor((p % 1000) / 100)
    const tens = Math.floor((p % 100) / 10)
    const ones = p % 10
    
    let s = ''
    if (thousands > 0) s += digits[thousands] + '千'
    if (hundreds > 0) {
      s += digits[hundreds] + '百'
    } else if (thousands > 0 && (tens > 0 || ones > 0)) {
      s += '零'
    }
    if (tens > 0) {
      s += digits[tens] + '十'
    } else if (hundreds > 0 && ones > 0) {
      s += '零'
    }
    if (ones > 0) {
      s += digits[ones]
    }
    return s
  }
  
  let result = ''
  for (let i = 0; i < parts.length; i++) {
    const part = parts[i]
    const bigUnit = bigUnits[parts.length - 1 - i]
    const partStr = partToZh(part)
    
    if (!partStr) {
      // 这一段是 0, 但不是最后一段, 需要零
      if (result && !result.endsWith('零') && i < parts.length - 1) {
        result += '零'
      }
      continue
    }
    
    // 前面部分 0, 这一段 0-999 开头, 加零
    if (result && part < 1000 && parts[i - 1] !== undefined) {
      // 上一段非 0, 这一段 < 1000, 需补零
      if (!result.endsWith('零')) result += '零'
    }
    
    result += partStr + bigUnit
  }
  
  return result || '零'
}

/**
 * 小数部分转中文 (逐位读)
 * - 0.56 → 五六
 */
function decToZh(decStr) {
  return decStr.split('').map(d => '零一二三四五六七八九'[+d]).join('')
}

/**
 * 数字转读法 (按上下文自动选)
 * - 默认中文
 * - 含英文 → 英文读法
 */
function numberToSpeech(numStr, lang = 'zh') {
  // 兼容 number/string 入参
  const s = String(numStr)
  if (lang === 'en') {
    return numberToEn(s)
  }
  // 中文
  const [int, dec] = s.split('.')
  let result = intToZh(parseInt(int, 10))
  if (dec) {
    result += '点' + decToZh(dec)
  }
  return result
}

/**
 * 英文数字读法 (简化版)
 */
function numberToEn(numStr) {
  // 简单: 数字串逐位读 (TTS 引擎一般能处理)
  return numStr.split('').join(' ')
}

/**
 * 金额转中文
 * - ¥1,234.56 → 1234 元 5 角 6 分
 * - $1,234 → 1234 美元
 * - 100元 → 100 元
 */
function moneyToZh(text) {
  // ¥/$/￥/€ + 数字
  text = text.replace(/([¥￥$€£])\s*([0-9][0-9,.]*)/g, (m, sym, num) => {
    const cleanNum = num.replace(/,/g, '')
    const cur = { '¥': '元', '￥': '元', '$': '美元', '€': '欧元', '£': '英镑' }[sym] || '元'
    
    if (cleanNum.includes('.')) {
      const [int, dec] = cleanNum.split('.')
      const jiao = dec[0] || '0'
      const fen = dec[1] || '0'
      return intToZh(parseInt(int, 10)) + cur + ' ' + jiao + ' 角 ' + fen + ' 分'
    }
    return intToZh(parseInt(cleanNum, 10)) + cur
  })
  
  // 100元 / 100 元 / 100.5元
  text = text.replace(/([0-9][0-9,.]*)\s*元/g, (m, num) => {
    const cleanNum = num.replace(/,/g, '')
    if (cleanNum.includes('.')) {
      const [int, dec] = cleanNum.split('.')
      return intToZh(parseInt(int, 10)) + '元 ' + decToZh(dec) + ' 分'
    }
    return intToZh(parseInt(cleanNum, 10)) + '元'
  })
  
  return text
}

/**
 * 百分比转中文
 * - 50% → 百分之五十
 * - 12.5% → 百分之十二点五
 */
function percentToZh(text) {
  return text.replace(/([0-9][0-9,.]*)\s*%/g, (m, num) => {
    const cleanNum = num.replace(/,/g, '')
    return '百分之' + numberToSpeech(cleanNum)
  })
}

/**
 * 日期转中文
 * - 2026-08-03 → 2026 年 8 月 3 日
 * - 2026/8/3 → 2026 年 8 月 3 日
 */
function dateToZh(text) {
  return text.replace(/(\d{4})[-\/.](0?[1-9]|1[0-2])[-\/.](0?[1-9]|[12]\d|3[01])/g, (m, y, mo, d) => {
    return numberToSpeech(y) + '年' + numberToSpeech(parseInt(mo, 10)) + '月' + numberToSpeech(parseInt(d, 10)) + '日'
  })
}

/**
 * 时间转中文
 * - 14:30 → 14 点 30 分
 * - 9:00 → 9 点
 */
function timeToZh(text) {
  return text.replace(/([01]?\d|2[0-3]):([0-5]\d)/g, (m, h, min) => {
    if (min === '00') return numberToSpeech(parseInt(h, 10)) + '点'
    return numberToSpeech(parseInt(h, 10)) + '点' + numberToSpeech(parseInt(min, 10)) + '分'
  })
}

/**
 * 手机号分段
 * - 18812345678 → 1 8 8 1 2 3 4 5 6 7 8
 * - 188-1234-5678 → 188 1234 5678
 */
function phoneToSpeech(text) {
  // 1xxxxxxxxxx (11 位手机号)
  text = text.replace(/\b(1[3-9]\d)(\d{4})(\d{4})\b/g, '$1 $2 $3')
  // xxx-xxxx-xxxx (带 - 的)
  text = text.replace(/\b(\d{3,4})-(\d{3,4})-(\d{4})\b/g, (m, a, b, c) => numberToSpeech(a) + ' ' + numberToSpeech(b) + ' ' + numberToSpeech(c))
  return text
}

/**
 * URL 字母化
 * - https://example.com → h-t-t-p-s-:-/-/-e-x-a-m-p-l-e-.-c-o-m
 * - 简化: 整体字母逐个读
 */
function urlToSpeech(text) {
  return text.replace(/(https?:\/\/[^\s]+)/g, (m) => m.split('').join(' '))
}

/**
 * 邮箱字母化
 * - a@b.com → a at b dot com
 */
function emailToSpeech(text) {
  return text.replace(/([\w.-]+)@([\w.-]+)\.([a-z]{2,})/gi, (m, user, dom, ext) => {
    return user.split('').join(' ') + ' at ' + dom.split('').join(' ') + ' dot ' + ext.split('').join(' ')
  })
}

/**
 * 单位词替换
 * - 5kg → 5 千克
 * - 100℃ → 100 摄氏度
 */
function unitsToSpeech(text) {
  // 长单位优先 (km² > m²)
  const sortedUnits = Object.keys(UNITS).sort((a, b) => b.length - a.length)
  for (const u of sortedUnits) {
    // 数字 + 单位 (前面必须是数字或空格)
    const re = new RegExp(`([0-9.\\u4e00-\\u9fa5]+)\\s?(${u.replace(/[²³°]/g, '\\$&')})(?![\\w])`, 'g')
    text = text.replace(re, (m, num, _u) => numberToSpeech(String(num)) + UNITS[u])
  }
  return text
}

/**
 * 大数字简化 (避免太长的中文读法)
 * - 12345678 → 1234 万 5678
 */
function bigNumberToSpeech(text) {
  return text.replace(/\b(\d{5,})\b/g, (m, num) => {
    const n = parseInt(num, 10)
    if (n < 10000) return m
    if (n < 100000000) {
      const wan = Math.floor(n / 10000)
      const rest = n % 10000
      return intToZh(wan) + '万' + (rest > 0 ? intToZh(rest) : '')
    }
    const yi = Math.floor(n / 100000000)
    const rest = n % 100000000
    return intToZh(yi) + '亿' + (rest > 0 ? bigNumberToSpeech(String(rest)) : '')
  })
}

/**
 * 主函数: 规范化 TTS 文本
 */
export function ttsNormalize(text, _options = {}) {
  // const { lang = 'zh' } = options  // V3.7.38+ reserved for multilingual TTS
  if (!text) return ''
  
  let result = text
  
  // 0. URL/邮箱优先 (避免和数字冲突)
  result = emailToSpeech(result)
  result = urlToSpeech(result)
  
  // 1. 金额 (¥/$/元/美元)
  result = moneyToZh(result)
  
  // 2. 日期 / 时间
  result = dateToZh(result)
  result = timeToZh(result)
  
  // 3. 百分比
  result = percentToZh(result)
  
  // 4. 手机号
  result = phoneToSpeech(result)
  
  // 5. 单位
  result = unitsToSpeech(result)
  
  // 6. 大数字 (5+ 位)
  result = bigNumberToSpeech(result)
  
  // 7. 余下纯数字 (1-4 位) - 让 TTS 引擎自己读
  // (不处理, TTS 引擎对短数字还行)
  
  return result
}

/**
 * 检测是否包含数字/金额/日期等 (用于决定是否调用 normalize)
 */
export function hasTtsContent(text) {
  if (!text) return false
  return /[0-9¥$€£%℃]/.test(text)
}

export default ttsNormalize
