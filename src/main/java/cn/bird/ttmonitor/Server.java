package cn.bird.ttmonitor;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bird.ttmonitor.model.Activity;
import cn.bird.ttmonitor.model.ActivityItem;
import cn.bird.ttmonitor.model.Category;
import cn.bird.ttmonitor.model.ItemDetail;
import cn.bird.ttmonitor.service.QueryService;
import cn.bird.ttmonitor.util.ConfigUtil;

public class Server {
	public static final Logger logger = LoggerFactory.getLogger(Server.class);
	
	public static void main(String[] args) throws Exception{
		if(!ConfigUtil.init("server.properties")){
			logger.error("fail to load configuration");
			return;
		}
		logger.info("server starting...");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String day = sdf.format(new Date());
		long currentTime = System.currentTimeMillis() / 1000;
		String dataDir = ConfigUtil.config.getString("tt.data_dir");
		String categoryFile = dataDir + File.separator + day + "_" + currentTime + "_category.txt";
		String activityFile = dataDir + File.separator + day + "_" + currentTime + "_activity.txt";
		String itemFile = dataDir + File.separator + day + "_" + currentTime + "_item.txt";
		String imageUrlFile = dataDir + File.separator + day + "_" + currentTime + "_imageurl.txt";
		FileWriter categoryWriter = new FileWriter(categoryFile, false);
		FileWriter activityWriter = new FileWriter(activityFile, false);
		FileWriter itemWriter = new FileWriter(itemFile, false);
		FileWriter imageUrlWriter = new FileWriter(imageUrlFile, false);
		
		try{
			Set<Integer> targetCategorySet = new HashSet<Integer>();
			Set<Integer> targetActivitySet = new HashSet<Integer>();
			List<Object> tempList = ConfigUtil.config.getList("tt.target_category_list");
			for(Object item : tempList){
				try{
					targetCategorySet.add(Integer.parseInt((String)item));
				}catch(Exception e){
					continue;
				}
				
			}
			
			QueryService queryService = new QueryService(ConfigUtil.config.getString("tt.cookie"), ConfigUtil.config.getString("tt.activity_url"),
					ConfigUtil.config.getString("tt.activity_items_url"), ConfigUtil.config.getString("tt.item_url"));
			List<Category> categoryList = queryService.getCategoryList();
			if(categoryList == null){
				//try again...
				categoryList = queryService.getCategoryList();
				if(categoryList == null){
					logger.error("stop,fail to getCategoryList");
					return;
				}
			}
			
			boolean imageFlag = false;
			for(Category category : categoryList){
				if(targetCategorySet.contains(category.getId())){
					imageFlag = true;
				}else{
					imageFlag = false;
				}
				categoryWriter.write(String.format("%s|%s|%s\n", day, category.getId(), category.getName()));
				List<Activity> activityList = queryService.getActivityList(category.getId());
				if(activityList == null){
					//try again...
					activityList = queryService.getActivityList(category.getId());
					if(activityList == null){
						logger.error("fail to getActivityList for category:{}" + category.getId());
						continue;
					}
				}
				for(Activity activity : activityList){
					activityWriter.write(String.format("%s|%s|%s|%s|%s|%s\n", day, category.getId(), activity.getId(), activity.getName(), activity.getStartTime(), activity.getEndTime()));
					int displayMode = 0;
					int pageIndex = 1;
					while(true){
						if(pageIndex == 1){
							displayMode = 0;
						}else{
							displayMode = 1;
						}
						
						List<ActivityItem> itemList = queryService.getActivityItemList(activity, displayMode, pageIndex, 20);
						if(itemList == null){
							//try again...
							itemList = queryService.getActivityItemList(activity, displayMode, pageIndex, 20);
							if(itemList == null){
								logger.error("fail to getActivityItemList for activity:{},page:{}", activity.getId(), pageIndex);
								continue;
							}
						}
						if(itemList.size() > 0){
							for(ActivityItem item : itemList){
								itemWriter.write(String.format("%s|%s|%s|%s|%s|%s|%s|%s\n", day, category.getId(),activity.getId(),item.getId(),
										item.getDealCount(),item.getTotalQty(),item.getPrice().toPlainString(),item.getTitle()));
								if(imageFlag){
									ItemDetail itemDetail = queryService.getItemDetail(item.getId(), activity.getId());
									if(itemDetail == null){
										itemDetail = queryService.getItemDetail(item.getId(), activity.getId());
									}
									if(itemDetail != null){
										for(String imageUrl : itemDetail.getImages()){
											int i = imageUrl.lastIndexOf(".");
											int j = imageUrl.lastIndexOf(":");
											if(i >= 0 && j >= 0){
												String postfix = imageUrl.substring(i+1);
												String urlPath = imageUrl.substring(j+1);
												String fullImageUrl = "https://nahuo-img-server.b0.upaiyun.com" + urlPath;
												imageUrlWriter.write(String.format("%s|%s\n", itemDetail.getId(), fullImageUrl));
											}
										}
									}
								}
								
							}
							pageIndex++;
							try {
								Thread.sleep(1000 * 3);
							} catch (InterruptedException e) {
								logger.error("exception", e);
							}
						}else{
							break;
						}
					}
				}
				Thread.sleep(1000 * 5);
			}
		}finally{
			categoryWriter.close();
			activityWriter.close();
			itemWriter.close();
			imageUrlWriter.close();
			logger.info("server stopped");
		}
	}
}
