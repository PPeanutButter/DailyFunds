# DailyFunds
安卓端显示基金估值的桌面小部件
![](widget.jpg)
需要配合Crontab实现数据更新，如：`0 9-15 * * 1-5 am broadcast -n com.peanut.gd.jj/.JJWidgetProvider -a android.appwidget.action.APPWIDGET_UPDATE`
