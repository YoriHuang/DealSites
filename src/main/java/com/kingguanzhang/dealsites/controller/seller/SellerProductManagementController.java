package com.kingguanzhang.dealsites.controller.seller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.kingguanzhang.dealsites.dto.Msg;
import com.kingguanzhang.dealsites.pojo.*;
import com.kingguanzhang.dealsites.service.FavoriteProductService;
import com.kingguanzhang.dealsites.service.ProductCategoryService;
import com.kingguanzhang.dealsites.service.ProductService;
import com.kingguanzhang.dealsites.service.ShopService;
import com.kingguanzhang.dealsites.util.RequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequestMapping("/seller")
@Controller
public class SellerProductManagementController {


    @Autowired
    private ProductService productService;

    @Autowired
    private ProductCategoryService productCategoryService;

    @Autowired
    private FavoriteProductService favoriteProductService;
    @Autowired
    private ShopService shopService;

    /**
     * 跳转到商品管理页风格一
     * @return
     */
    @RequestMapping("/product/managementPage1")
    public String showProductManagement(HttpServletRequest request,Model model){
        PersonInfo personInfo = (PersonInfo) request.getSession().getAttribute("personInfo");
        Shop shop = shopService.getShopByUserId(personInfo.getUserId());
        model.addAttribute("shopId",shop.getShopId());
        return "seller/productManagement1";
    }

    /**
     * 跳转到商品管理页风格二
     * @return
     */
    @RequestMapping("/product/managementPage2")
    public String showProductManagement2(HttpServletRequest request,Model model){
        PersonInfo personInfo = (PersonInfo) request.getSession().getAttribute("personInfo");
        Shop shop = shopService.getShopByUserId(personInfo.getUserId());
        model.addAttribute("shopId",shop.getShopId());
        return "seller/productManagement2";
    }

    /**
     * 跳转到新增商品页;
     * @return
     */
    @RequestMapping("/product/addProductPage")
    public String addProduct(){
        return "seller/addProduct";
    }

    /**
     * 跳转到编辑商品页
     * @param productId
     * @param model
     * @return
     */
    @RequestMapping("/product/editProductPage/{productId}")
    public String showEditProduct(@PathVariable("productId") Integer productId, Model model) {
       /* //记得要将id写入session,为了以后更新商品时方便取出Id
        request.getSession().setAttribute("productId",productId);*/
       //发现写入session的话在以后同时编辑两个商品时会出现session值被覆盖的情况,所以改用js在前端元素中取id然后用get或post传参;
        Product product = productService.getProduct(productId);
        model.addAttribute("product",product);
        List<ProductCategory> productCategoryList = productCategoryService.getCategory();
        model.addAttribute("productCategoryList",productCategoryList);
        return "/seller/editProduct";
    }




     /**
     *通过分类名获取分类下所有在售的商品;
     * @param categoryId
     * @param pn
     * @return
     */
    @RequestMapping(value = "/ajax/product/allOnSalesByCategoryId",method = RequestMethod.POST)
    @ResponseBody
    public Msg getOnSellProductListByCategoryId(@RequestParam("categoryId") Integer categoryId, @RequestParam(value = "pn",defaultValue = "1") Integer pn,HttpServletRequest request) {
        PageHelper.startPage(pn,8);
        List<Product> productList = productService.getOnSellProductListByCategoryId(categoryId);
        PageInfo pageInfo = new PageInfo(productList,5);
        Msg msg =Msg.success().setMsg("获取商品集合成功").add("pageInfo", pageInfo);

        //查询出用户收藏的商品的Id,为了在首页的商品卡牌中判断是显示收藏还是取消收藏按钮;
        PersonInfo personInfo = (PersonInfo) request.getSession().getAttribute("personInfo");
        if (null != personInfo){
            List<FavoriteProduct> favoriteProductList = favoriteProductService.getFavoriteProductList(personInfo.getUserId());
            msg.add("favoriteProductList",favoriteProductList);
        }

        return msg;
    }

    /**
     *通过分类名和店铺Id获取分类下所有在售商品;
     * @param categoryId
     * @param pn
     * @return
     */
    @RequestMapping(value = "/ajax/product/allOnSalesByShopIdAndCategoryId",method = RequestMethod.POST)
    @ResponseBody
    public Msg getOnSellProductListByShopIdAndCategoryId(@RequestParam("categoryId") Integer categoryId,@RequestParam("shopId") Integer shopId, @RequestParam(value = "pn",defaultValue = "1") Integer pn, HttpServletRequest request) {
        PageHelper.startPage(pn,8);
        List<Product> productList = productService.getOnSellProductListByCategoryIdAndShopId(categoryId,shopId);
        PageInfo pageInfo = new PageInfo(productList,5);
        Msg msg =Msg.success().setMsg("获取商品集合成功").add("pageInfo", pageInfo);

        //查询出用户收藏的商品的Id,为了在首页的商品卡牌中判断是显示收藏还是取消收藏按钮;
        PersonInfo personInfo = (PersonInfo) request.getSession().getAttribute("personInfo");
        if (null != personInfo){
            List<FavoriteProduct> favoriteProductList = favoriteProductService.getFavoriteProductList(personInfo.getUserId());
            msg.add("favoriteProductList",favoriteProductList);
        }

        return msg;
    }

    /**
     * 通过ajax调用的批量下架商品的方法;
     *
     * @param productIds
     * @return
     */
    @RequestMapping(value = "/ajax/product/soleOutBatch", method = RequestMethod.POST)
    @ResponseBody
    public Msg soldoutProducts(@RequestParam("productIds") String productIds) {
        String[] productIdArray = productIds.split(",");
        List<Integer> productIdList = new ArrayList();
        for (String productId:productIdArray ) {
            productIdList.add(Integer.parseInt(productId));
        }
        int i = 0;
        try{
            i = productService.soldoutProducts(productIdList);
        }catch (Exception e){
            e.printStackTrace();
            return Msg.fail().setMsg("下架商品失败");
        }
        if (0 < i) {
            return Msg.success().setMsg("下架成功!");
        }
        return Msg.fail().setMsg("下架失败");
    }

    /**
     * 通过ajax调用的批量上架商品的方法;
     *
     * @param productIds
     * @return
     */
    @RequestMapping(value = "/ajax/product/putAwayBatch", method = RequestMethod.POST)
    @ResponseBody
    public Msg putawayProducts(@RequestParam("productIds") String productIds) {
        String[] productIdArray = productIds.split(",");
        List<Integer> productIdList = new ArrayList();
        for (String productId:productIdArray ) {
            productIdList.add(Integer.parseInt(productId));
        }
        int i = 0;
        try{
             i = productService.putawayProducts(productIdList);
        }catch (Exception e){
            e.printStackTrace();
            return Msg.fail().setMsg("上架失败");
        }
        if (0 < i) {
            return Msg.success().setMsg("上架成功!");
        }
        return Msg.fail().setMsg("上架失败");
    }

    /**
     * 通过ajax调用的批量删除商品的方法;
     *
     * @param productIds
     * @return
     */
    @RequestMapping(value = "/ajax/product/deleteBatch", method = RequestMethod.POST)
    @ResponseBody
    public Msg deleteProducts(@RequestParam("productIds") String productIds) {
        String[] productIdArray = productIds.split(",");
        List<Integer> productIdList = new ArrayList();
        for (String productId:productIdArray ) {
                productIdList.add(Integer.parseInt(productId));
        }
        int i = 0;
        try{
          i = productService.deleteProducts(productIdList);
        }catch (Exception e){
            e.printStackTrace();
            return Msg.fail().setMsg("删除失败");
        }
        if (0 < i) {
            return Msg.success().setMsg("删除成功!");
        }
        return Msg.fail().setMsg("删除失败");
    }

    /**
     * 通过ajax调用的删除商品的方法;
     *
     * @param productId
     * @return
     */
    @RequestMapping(value = "/ajax/product/delete", method = RequestMethod.POST)
    @ResponseBody
    public Msg removeProduct(@RequestParam("productId") Integer productId) {
        int i = 0;
        try{
            i = productService.removeProduct(productId);
        }catch (Exception e){
            e.printStackTrace();
            return Msg.fail().setMsg("删除失败");
        }
        if (0 < i) {
            return Msg.success().setMsg("删除成功!");
        }
        return Msg.fail().setMsg("删除失败");
    }

    /**
     * 通过ajax调用的上架商品的方法;
     *
     * @param productId
     * @return
     */
    @RequestMapping(value = "/ajax/product/putAway", method = RequestMethod.POST)
    @ResponseBody
    public Msg putAwayProduct(@RequestParam("productId") Integer productId) {
        int i = 0;
        try{
             i = productService.shelveProduct(productId);
        }catch (Exception e){
            e.printStackTrace();
            return Msg.fail().setMsg("上架失败");
        }
        if (0 < i) {
            return Msg.success().setMsg("上架成功!");
        }
        return Msg.fail().setMsg("上架失败");
    }

    /**
     * 通过ajax调用的下架商品的方法;
     *
     * @param productId
     * @return
     */
    @RequestMapping(value = "/ajax/product/soldOut", method = RequestMethod.POST)
    @ResponseBody
    public Msg soldOutProduct(@RequestParam("productId") Integer productId) {
        int i = 0;
        try{
            i = productService.unShelveProduct(productId);
        }catch (Exception e){
            e.printStackTrace();
            return Msg.fail().setMsg("下架失败");
        }
        if (0 < i) {
            return Msg.success().setMsg("下架成功!");
        }
        return Msg.fail().setMsg("下架失败");
    }

    /**
     * ajax查询店铺内所有的商品列表;
     *
     * @return
     */
    @RequestMapping(value = "/ajax/product/allByShopId",method = RequestMethod.POST)
    @ResponseBody
    public Msg getProductList(@RequestParam(value = "pn",defaultValue = "1")Integer pn,@RequestParam("shopId")Integer shopId) {
        //使用分页插件官方推荐的第二种方式开启分页查询;
        PageHelper.startPage(pn, 8);
        //然后紧跟的查询就是分页查询;
        List<Product> productList = productService.getProductList(shopId);
        //查询之后使用PageInfo来包装,方便在页面视图中处理页码,下面用的构造器第二个参数是页面底部可供点击的连续页码数;
        PageInfo pageInfo = new PageInfo(productList,5);
        return Msg.success().setMsg("获取商品集合成功").add("pageInfo", pageInfo);
    }

    /**
     * ajax查询店铺内所有上架中的商品列表,此方法与common中的一个方法一样,但为了方便以后升级所以两份都保留;
     * @param request
     * @return
     */
    @RequestMapping(value = "/ajax/product/allOnSalesByShopId",method = RequestMethod.POST)
    @ResponseBody
    public Msg getShelveProductList(@RequestParam(value = "pn",defaultValue = "1")Integer pn,@RequestParam(value = "shopId")Integer shopId, HttpServletRequest request) {
        //使用分页插件官方推荐的第二种方式开启分页查询;
        PageHelper.startPage(pn, 8);
        //然后紧跟的查询就是分页查询;
        List<Product> productList = productService.getShelveProductList(shopId);
        //查询之后使用PageInfo来包装,方便在页面视图中处理页码,下面用的构造器第二个参数是页面底部可供点击的连续页码数;
        PageInfo pageInfo = new PageInfo(productList,5);
        Msg msg =Msg.success().setMsg("获取商品集合成功").add("pageInfo", pageInfo);

        //查询出用户收藏的商品的Id,为了在首页的商品卡牌中判断是显示收藏还是取消收藏按钮;
        PersonInfo personInfo = (PersonInfo) request.getSession().getAttribute("personInfo");
        if (null != personInfo){
            List<FavoriteProduct> favoriteProductList = favoriteProductService.getFavoriteProductList(personInfo.getUserId());
            msg.add("favoriteProductList",favoriteProductList);
        }

        return msg;
    }

    /**
     * ajax查询店铺内所有下架中的商品列表;
     * @param request
     * @return
     */
    @RequestMapping(value = "/ajax/product/allHaltSalesByShopId",method = RequestMethod.POST)
    @ResponseBody
    public Msg getUnShelveProduct(@RequestParam(value = "pn",defaultValue = "1")Integer pn,@RequestParam(value = "shopId")Integer shopId, HttpServletRequest request) {
        //使用分页插件官方推荐的第二种方式开启分页查询;
        PageHelper.startPage(pn, 8);
        //然后紧跟的查询就是分页查询;
        List<Product> productList = productService.getUnShelveProduct(shopId);
        //查询之后使用PageInfo来包装,方便在页面视图中处理页码,下面用的构造器第二个参数是页面底部可供点击的连续页码数;
        PageInfo pageInfo = new PageInfo(productList,5);
        return Msg.success().setMsg("获取商品集合成功").add("pageInfo", pageInfo);
    }

    /**
     * ajax查询单个商品详情
     * @param request
     * @return
     */
    @RequestMapping(value = "/ajax/product/get",method = RequestMethod.GET)
    @ResponseBody
    public Msg getProduct(HttpServletRequest request) {
        //从session中获取商品Id,页面和js中也不要暴露商品id,防止用户修改id
        Integer productId = (Integer) request.getSession().getAttribute("productId");
        Product product = productService.getProduct(productId);
        return Msg.success().setMsg("获取商品集合成功").add("product", product);
    }

    /**
     * ajax更新商品信息
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/ajax/product/update", method = RequestMethod.POST)
    @ResponseBody
    public Msg updateProduct(HttpServletRequest request) {

        //从前端传来的请求中获取键为productStr的值;
        String productStr = RequestUtil.parserString(request, "productStr");
        ObjectMapper objectMapper = new ObjectMapper();
        Product product = null;

        try {
            //将前端传来的商品信息转换为product实体类;
            System.out.print("productStr的值是:" + productStr);
            product = objectMapper.readValue(productStr, Product.class);

            /*这里需要注意,productId需要小心处理,建议页面上一步查询时就写入session,防止用户在前端修改id导致处理了错误的数据;*/
            int productId = (int) request.getSession().getAttribute("productId");//从session中取出商品id
            product.setProductId(productId);

        } catch (Exception e) {
            e.printStackTrace();
            return Msg.fail().setMsg("商品信息不能正确解析");
        }


        //从request中解析出上传的文件图片;
        MultipartFile productImg = ((MultipartRequest) request).getFile("shopImg");

        //判断是否需要更新图片;
        if (null != productImg) {
            try {
                //使用文件.getOriginalFilename可以获取带后缀.jpg的全名;或者文件.getItem.getName也可以获取带后缀的文件名;否则只能取到不带后缀的文件名;
                productService.updateProductWithImg(product, productImg.getInputStream(), productImg.getOriginalFilename());
            } catch (IOException e) {
                System.out.print( "异常信息"+e.getMessage());
                return Msg.fail().setMsg("更新商品信息失败");
            }
        }
        int i = productService.updateProduct(product);
        if (i >= 0) {
            product = productService.getProduct(product.getProductId());
            return Msg.success().setMsg("更新商品成功").add("product", product);
        }
        return Msg.fail().setMsg("更新商品信息失败");
    }

    /**
     * ajax新增商品的方法;
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/ajax/product/add", method = RequestMethod.POST)
    @ResponseBody
    private Msg addProduct(HttpServletRequest request) {

        //从前端传来的请求中获取键为productStr的值;
        String productStr = RequestUtil.parserString(request, "productStr");
        ObjectMapper objectMapper = new ObjectMapper();
        Product product = null;
        Integer shopId = (Integer) request.getSession().getAttribute("shopId");

        try {
            //将前端传来的商店信息转换为product实体类;
            System.out.print("productStr的值是:" + productStr);
            product = objectMapper.readValue(productStr, Product.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Msg.fail().setMsg("设置商品信息失败!");
        }

        //从request中解析出上传的文件图片;
        MultipartFile productImg = ((MultipartRequest) request).getFile("shopImg");
        //新增商品,尽可能的减少从前端获取的值;
        if (null != product && null != productImg) {
          //  product.setShopId(shopId);//这个shopid必须从session中获取,这是为了安全
            try {
                //使用文件.getOriginalFilename可以获取带后缀.jpg的全名;或者文件.getItem.getName也可以获取带后缀的文件名;否则只能取到不带后缀的文件名;
                productService.addProduct(product, productImg.getInputStream(), productImg.getOriginalFilename());
            } catch (IOException e) {
                System.out.print("异常信息"+e.getMessage());
                return Msg.fail().setMsg("图片保存出错了");
            }
            //返回新增商品的最终结果;
            return Msg.success().setMsg("添加成功");
        } else {
            return Msg.fail().setMsg("添加失败,商品信息不完整!");
        }

    }

}
