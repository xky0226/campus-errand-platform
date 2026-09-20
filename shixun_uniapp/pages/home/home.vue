<template>
	<view>
		<h1>{{username}}</h1>
		<h1>home-首页</h1>
		<button @click="test">按钮</button>
	</view>
</template>

<!-- js代码 -->
<script>
	export default {
		//数据集【和template中的标签进行数据的双向绑定】
		data() {
			return {
				title: 'Hello',
				username: 'zhangsan'
			}
		},
		//生命周期
		onLoad() {
			//数据查询、数据预处理的操作
			console.log("发起查询，加载列表数据！！！");
		},
		//函数集
		methods: {
			test() {
				console.log("点击事件触发！！！");
			},
		}
	}
</script>

<style>

</style>
