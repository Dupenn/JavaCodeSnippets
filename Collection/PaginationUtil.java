import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

/**
 * 数据分页工具类
 *
 * @author dupeng
 * @date 2021/2/19 11:21
 **/
public class PaginationUtil {
    /**
     * 数据分页
     *
     * @param list     待分页数据列表
     * @param pageSize 每页大小
     * @param page     第几页，首页为1
     * @param <T>      待分页的数据类型
     * @return 分页数据
     */
    public static <T> List<T> partition(List<T> list, Integer pageSize, Integer page) {
        // 每页数量为空返回所有的，不进行分页
        if (list.isEmpty() || pageSize == null) {
            return new ArrayList<>();
        }
        int size = list.size();
        Map<Integer, List<T>> allData = IntStream.iterate(0, i -> i + pageSize)
                //循环需要页面尺寸  如pageSize=10 ,它就会10 20 30 40 50的一直循环
                .limit((size + pageSize - 1) / pageSize)
                //限制循环的次数  list的大小+页的大小  / 页的大小
                .boxed()
                //看实现方法 把int包装成了对象
                .collect(toMap(i -> i / pageSize,
                        //返回结构 key当前页数   i包装对象后的Integer是当前循环的页数大小
                        i -> list.subList(i, min(i + pageSize, size))));
        //list分页 subList(取当前循环的指针,(a <= b)：如果是返回当前指针+分页的大小?:如果不是则当前的指针大小到最后一个指针的数据)
        // 默认返回第一页
        if (page == null || page.equals(1)) {
            return allData.get(0);
        }
        // 获取总页数
        double totalPage = Math.ceil((double) size / pageSize.doubleValue());
        // 请求数据超过最大页面
        if (page > (int) totalPage - 1) {
            return allData.get((int) totalPage - 1);
        }
        return allData.get(page - 1);
    }
}
