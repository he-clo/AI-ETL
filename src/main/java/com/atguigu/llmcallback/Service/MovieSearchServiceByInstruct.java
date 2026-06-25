package com.atguigu.llmcallback.Service;

//@Service
//public class MovieSearchServiceByInstruct {
//    @Autowired
//    private MovieVectorRepository movieVectorRepository;
//
//    @Value("${ollama.base-url}")
//    private String baseUrl;
//
//    @Value("${ollama.model.embed}")
//    private String embedModel;
//
//    @Value("${ollama.timeout-ms}")
//    private int timeoutMs;
//
//    @Autowired
//    private RestTemplate restTemplate;
//
//    public List<Movie> searchSimilarMovies(String userQuery) {
//        // 1. 【模拟】将用户的文本查询转换为向量 (实际应用中这里会调用 EmbeddingModel)
//       // float[] queryVector = generateMockEmbedding(userQuery);
//
//        // 2. 调用 Repository 进行数据库相似度检索
//        List<Movie> results = movieVectorRepository.findTop10Similar(userQuery);
//
//        // 3. 【可选】后处理，例如过滤相似度低于阈值的电影
//        // results = results.stream().filter(m -> m.getSimilarity() > 0.8).toList();
//
//        return results;
//    }
//
//
//    /**
//     * 发送 POST 请求，参数是 text，返回 float[]
//     */
//    public float[] generateMockEmbedding(String text) {
//        String url = baseUrl + "/api/embeddings";
//
//        // 请求体
//        Map<String, Object> body = Map.of(
//                "model", embedModel,
//                "prompt", "Retrieve relevant movie: "+ text //"prompt", "检索相关电影：" + text
//        );
//
//        // Header
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<Map<String, Object>> entity =
//                new HttpEntity<>(body, headers);
//
//        try {
//            // 发送 POST
//            ResponseEntity<Map> response =
//                    restTemplate.postForEntity(url, entity, Map.class);
//
//            if (response.getStatusCode().is2xxSuccessful()
//                    && response.getBody() != null) {
//
//                List<Number> embedding =
//                        (List<Number>) response.getBody().get("embedding");
//
//                float[] result = new float[embedding.size()];
//                for (int i = 0; i < embedding.size(); i++) {
//                    result[i] = embedding.get(i).floatValue();
//                }
//                return result;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return new float[0];
//    }
//}
