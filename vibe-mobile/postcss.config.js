export default {
  plugins: {
    'postcss-px-to-viewport-8-plugin': {
      // 设计稿宽度 375px（iPhone 标准）
      viewportWidth: 375,
      unitPrecision: 5,
      viewportUnit: 'vw',
      selectorBlackList: ['.ignore-vw', '.van-popup', '.van-toast', '.van-dialog'],
      minPixelValue: 1,
      mediaQuery: false,
      // 排除 Vant 默认 375 设计稿导致的二次转换
      exclude: [/node_modules\/(?!vant)/]
    }
  }
}
