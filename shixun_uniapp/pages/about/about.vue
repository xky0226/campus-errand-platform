<template>
	<view>
		<h1>12456</h1>
		<h1>{{title}}</h1>
		<swiper :indicator-dots="true" :autoplay="true" :interval="3000" :duration="1000">
			<!-- 2.从数据源中获取图片数据 使用v-for遍历 获取图片数据 -->
			<!-- index 数组的索引 下标 item 条目 一个个对象 :key="index" :冒号 绑定数据 -->
			<swiper-item v-for="item,key in pictures" :key="key">
				<image :src="item.url" mode="" class="pic" @click="toNavelList">
				</image>
				swiper
				滑块视图容器。
				一般用于左右滑动或上下滑动，比如banner轮播图。
				注意：滑动切换和滚动的区别，滑动切换是一屏一屏的切换。
				swiper下的每个swiper-item是一个滑动切换区域，不能停留在2个滑动区域之间。
			</swiper-item>
		</swiper>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				title: 'zhangsan',
				pictures: [{
						id: '0',
						url: '../../static/image/1.png'
					},
					{
						id: '1',
						url: '../../static/image/2.png'
					},
					{
						id: '2',
						url: '../../static/image/3.png'
					}
				],
			}
		},
		onLoad() {

		},
		methods: {
			toNavelList(){}
		}
	}
</script>

<style>

</style>
