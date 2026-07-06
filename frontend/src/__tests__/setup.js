// Vitest 全局设置
import { vi } from 'vitest'

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      warning: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
    },
  }
})

// Mock dayjs
vi.mock('dayjs', () => ({
  default: vi.fn((val) => val),
}))

// 全局 afterEach
import { afterEach } from 'vitest'
afterEach(() => {
  vi.clearAllMocks()
})
