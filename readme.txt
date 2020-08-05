git add 文件名
git commit -m "修改备注"
git status 					//查看状态
git diff 					//查看上次修改的具体情况（即还未提交的版本与最近一次提交的版本的区别）
git log 					//显示从最近到最远的提交日志
git log --pretty=oneline	//每个版本的提交日志只显示一行
git reset --hard HEAD^		//返回上一个版本，HEAD表示当前版本，再HEAD~5往前的第五个版本
git reset --hard 指定的版本ID（不一定要写全）
git reflog					//用来记录你的每一次命令
git checkout -- <file>		//丢弃工作区的修改：如果自修改后还没被放到暂存区，撤销修改回到与版本库一致的状态
											//  如果已添加到暂存区后，又作了修改，撤销修改回到添加到暂存区后的状态
							//总之，就是让这个文件回到最近一次git commit或git add时的状态
git reset HEAD <file>		//可以把暂存区的修改撤销掉（unstage），重新放回工作区（也就是只有工作区被修改）
git rm						//用于删除一个文件。如果一个文件已经被提交到版本库，那么你永远不用担心误删，但是要小心，你只能恢复文件到最新版本，你会丢失最近一次提交后你修改的内容
git remote add origin https://github.com/Henry-Zhuang-cn/OpenTCS.git	//将本地库与远程库连接（HTTP方式）速度较慢
git remote add origin git@github.com:Henry-Zhuang-cn/OpenTCS.git		//将本地库与远程库连接（SSH方式）
git push -u origin master	//把本地库的内容推送到远程，实际上是把当前分支master推送到远程
							//加上了-u参数，Git不但会把本地的master分支内容推送的远程新的master分支，还会把本地的master分支和远程的master分支关联起来，在以后的推送或者拉取时就可以简化命令