// Vitest global setup — runs once before all tests
// happy-dom environment handles window/document/navigator automatically
// This file handles additional global mocks.
import { vi } from 'vitest'

// Mock Element Plus (required by many Vue components under test)
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
    ElMessageBox: { confirm: vi.fn() },
  }
})
