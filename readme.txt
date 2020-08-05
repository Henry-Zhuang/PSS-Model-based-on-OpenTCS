git add 文件名
git commit -m "修改备注"
git status 					//查看状态
git diff 					//查看上次修改的具体情况
git log 					//显示从最近到最远的提交日志
git log --pretty=oneline	//每个版本的提交日志只显示一行
git reset --hard HEAD^		//返回上一个版本，HEAD表示当前版本，再HEAD~5往前的第五个版本
git reset --hard 指定的版本ID（不一定要写全）
git reflog					//用来记录你的每一次命令