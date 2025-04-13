# Spring Retry 使用筆記

Spring Retry 是一個用於處理失敗操作的重試機制框架，特別適用於可能因暫時性故障而失敗的操作（如網路連接、資料庫操作等）。這個專案提供了
Spring Retry 的基本使用範例。

## 專案環境

- Java 21
- Spring Boot 3.4.4
- Gradle 構建
- Spring Retry

## 專案結構

```
src/main/java/com/bill/springretry/
├── SpringRetryApplication.java (主應用程式入口點)
├── exception
│   ├── TransientNetworkException.java (自定義的暫時性網絡異常)
│   ├── DatabaseException.java (自定義的資料庫異常)
│   └── RemoteServiceException.java (自定義的遠程服務異常)
├── service
│   └── ExternalService.java (示範基本 @Retryable 的服務)
├── programmatic
│   ├── ProgrammaticRetryService.java (示範編程式重試的服務)
│   └── ProgrammaticRetryRunner.java (編程式重試的執行器)
├── stateful
│   ├── StatefulRetryService.java (示範有狀態重試的服務)
│   └── StatefulRetryRunner.java (有狀態重試的執行器)
├── custom
│   ├── HttpStatusCodeRetryPolicy.java (自定義 HTTP 狀態碼重試策略)
│   ├── TimeAwareBackOffPolicy.java (自定義時間感知退避策略)
│   ├── CustomRetryService.java (使用自定義重試策略的服務)
│   └── CustomRetryRunner.java (自定義重試策略的執行器)
├── listener
│   ├── RetryLoggingListener.java (重試日誌監聽器)
│   ├── RetryMetricsListener.java (重試度量監聽器)
│   ├── ListenerRetryService.java (使用監聽器的重試服務)
│   └── ListenerRetryRunner.java (監聽器重試的執行器)
├── circuitbreaker
│   ├── CircuitBreakerService.java (斷路器模式的重試服務)
│   └── CircuitBreakerRunner.java (斷路器模式的執行器)
└── configuration
    ├── RetryConfiguration.java (基於 Spring 配置的重試設置)
    ├── ConfiguredRetryService.java (使用配置的重試服務)
    └── ConfiguredRetryRunner.java (配置重試的執行器)
```

## Spring Retry 主要功能

### 1. 重試機制

Spring Retry 提供了一個簡單的重試機制，當操作失敗時會自動重試：

- **基於註解**：使用 `@Retryable` 標記需要重試的方法
- **自定義重試策略**：可以指定重試次數、延遲時間、特定的異常類型等
- **恢復處理**：通過 `@Recover` 指定在所有重試失敗後執行的方法

### 2. 關鍵註解

#### `@EnableRetry`

在應用程序配置類上標記，啟用 Spring Retry 功能：

```java
@EnableRetry
@SpringBootApplication
public class SpringRetryApplication {
    // ...
}
```

#### `@Retryable`

標記需要重試的方法，可配置以下屬性：

- `retryFor`/`include`：指定需要重試的異常類型
- `exclude`：指定不需要重試的異常類型
- `maxAttempts`：最大重試次數
- `backoff`：重試間隔配置

```java
@Retryable(
    retryFor = {TransientNetworkException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 2000)
)
public void callExternalService() {
    // 可能失敗的代碼
}
```

#### `@Recover`

定義在所有重試失敗後的恢復方法：

```java
@Recover
public void recover(TransientNetworkException e) {
    // 處理最終失敗的邏輯
}
```

#### `@Backoff`

定義重試間隔時間：

- `delay`：初始延遲時間（毫秒）
- `maxDelay`：最大延遲時間
- `multiplier`：延遲時間的乘數（用於指數退避）

### 3. 常見使用場景

- 網路請求（HTTP 調用、遠程服務調用）
- 資料庫操作（特別是在高併發環境下）
- 消息隊列操作
- 分佈式系統中的服務間通信

## 專案示例說明

本專案展示了以下重試機制的實現方式：

### 1. 基於註解的重試 (`service` 包)

- 使用 `@Retryable` 和 `@Recover` 註解實現重試和恢復邏輯
- 範例演示了網絡請求的重試

### 2. 編程式重試 (`programmatic` 包)

- 使用 `RetryTemplate` 以編程方式實現重試機制
- 展示了如何配置不同的重試策略和退避策略
- 演示了如何傳遞重試上下文並在重試過程中記錄信息

```java
RetryTemplate template = new RetryTemplate();
SimpleRetryPolicy policy = new SimpleRetryPolicy();
policy.setMaxAttempts(3);
template.setRetryPolicy(policy);

String result = template.execute(context -> {
    // 可能失敗的操作
    return callService();
});
```

### 3. 有狀態重試 (`stateful` 包)

- 演示了在多次調用間保持重試狀態
- 適用於需要跨多個請求或事務維護重試狀態的場景
- 使用 `RetryState` 和 `DefaultRetryState` 實現

```java
RetryState retryState = new DefaultRetryState(transactionId);
return retryTemplate.execute(callback, recoveryCallback, retryState);
```

### 4. 自定義重試策略 (`custom` 包)

- 實現了基於 HTTP 狀態碼的自定義重試策略 `HttpStatusCodeRetryPolicy`
- 實現了根據當前時間調整退避間隔的 `TimeAwareBackOffPolicy`
- 展示了如何將這些自定義策略與 RetryTemplate 結合使用

### 5. 重試監聽器 (`listener` 包)

- 實現了 `RetryLoggingListener` 用於記錄重試事件
- 實現了 `RetryMetricsListener` 用於收集重試相關的度量數據
- 展示了如何通過監聽器實現重試過程的可觀測性

```java
RetryTemplate template = new RetryTemplate();
template.registerListener(new RetryLoggingListener());
template.registerListener(new RetryMetricsListener());
```

### 6. 斷路器模式 (`circuitbreaker` 包)

- 實現了斷路器模式，在系統故障時切斷請求
- 使用 `CircuitBreakerRetryPolicy` 包裝基本的重試策略
- 展示了斷路器的打開、半開和關閉狀態轉換

```java
CircuitBreakerRetryPolicy circuitBreakerRetryPolicy = new CircuitBreakerRetryPolicy(simpleRetryPolicy);
circuitBreakerRetryPolicy.setOpenTimeout(5000); // 斷路器打開後 5 秒內不允許新的請求
circuitBreakerRetryPolicy.setResetTimeout(10000); // 10 秒後自動重置斷路器狀態
```

### 7. 基於配置的重試 (`configuration` 包)

- 展示了如何使用 Spring 配置（`@Configuration`）創建不同類型的 `RetryTemplate` Bean
- 演示了如何在不同服務中注入和使用這些模板
- 用於需要在多個服務中共享重試策略的場景

## 如何使用本專案

### 運行不同的範例

本專案使用 Spring Boot Profile 控制執行哪種範例。使用以下參數運行相應的範例：

1. 基本範例：不需要特別的 profile
2. 編程式重試範例：`--spring.profiles.active=programmatic`
3. 有狀態重試範例：`--spring.profiles.active=stateful`
4. 自定義策略範例：`--spring.profiles.active=custom`
5. 監聽器範例：`--spring.profiles.active=listener`
6. 斷路器範例：`--spring.profiles.active=circuitbreaker`
7. 配置範例：`--spring.profiles.active=configuration`

## Spring Retry 的工作原理

Spring Retry 的核心是一個簡單的模板方法模式實現：

1. **RetryOperations 接口**：定義了重試操作的基本 API
2. **RetryTemplate**：提供了 RetryOperations 的基本實現
3. **RetryPolicy**：決定何時重試（如重試次數、特定異常等）
4. **BackOffPolicy**：決定重試之間的延遲策略
5. **RetryContext**：在重試過程中傳遞狀態
6. **RetryCallback**：包含要重試的實際操作
7. **RecoveryCallback**：當重試失敗時的恢復策略
8. **RetryListener**：監聽重試的各個環節

### 基本執行流程

1. 客戶端調用 `RetryTemplate.execute()`
2. RetryTemplate 創建一個 RetryContext
3. RetryTemplate 調用 RetryPolicy 來決定是否應該嘗試操作
4. 如果應該嘗試，則調用 RetryCallback 中的代碼
5. 如果操作成功，返回結果
6. 如果操作失敗，RetryTemplate 會記錄失敗
7. RetryTemplate 諮詢 RetryPolicy 決定是否應該重試
8. 如果應該重試，RetryTemplate 使用 BackOffPolicy 來決定等待多長時間
9. 在等待後，重複步驟 4-8
10. 如果達到最大重試次數，則調用 RecoveryCallback（如果提供）

## 進階用法

### 1. 組合多種重試策略

Spring Retry 允許組合多種重試策略，例如：

```java
// 組合重試策略
CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();
compositeRetryPolicy.setPolicies(new RetryPolicy[] {
    new TimeoutRetryPolicy(),  // 基於時間的策略
    new SimpleRetryPolicy()    // 基於次數的策略
});
```

### 2. 動態調整重試策略

在某些場景下，你可能需要根據運行時條件動態調整重試行為：

```java
RetryTemplate template = new RetryTemplate();
template.setRetryPolicy(new RetryPolicy() {
    @Override
    public boolean canRetry(RetryContext context) {
        // 根據外部條件或上下文決定是否重試
        return checkSomeExternalCondition() && context.getRetryCount() < getMaxAttemptsFromConfig();
    }
    // 實現其他必要方法...
});
```

### 3. 客製化後端儲存的有狀態重試

對於需要持久化重試狀態的場景（如跨JVM或跨重啟），可以實現自定義的儲存機制：

```java
// 自定義RetryState，使用資料庫或Redis等儲存重試狀態
RetryState customRetryState = new RetryState() {
    @Override
    public boolean isForceRefresh() {
        return false;
    }
    
    @Override
    public Object getKey() {
        return "custom-key";
    }
    
    // 可以通過資料庫或Redis讀取保存的重試狀態
};
```

## 最佳實踐

1. **僅針對暫時性故障重試**：只有那些有可能成功的操作才值得重試
2. **設置合理的最大重試次數**：避免無限重試導致資源浪費
3. **使用指數退避或隨機退避**：防止所有客戶端同時重試
4. **設置超時機制**：配合重試使用，避免長時間阻塞
5. **記錄重試事件**：便於監控和診斷問題
6. **提供降級處理**：通過 `@Recover` 提供優雅降級
7. **結合斷路器使用**：在系統大面積故障時快速失敗
8. **監控重試指標**：通過監聽器收集和監控重試相關數據
9. **優化退避策略**：根據業務場景選擇合適的退避策略
10. **注意線程安全**：在多線程環境中使用有狀態重試時確保線程安全

## 注意事項

- 重試機制會導致方法執行時間延長，需要在設計系統時考慮
- 針對非冪等操作重試需要特別小心（如增加訂單）
- 過多的重試可能會加重下游系統負擔
- 需要確保重試不會導致資源耗盡（數據庫連接、線程池等）
- 有狀態重試需要考慮狀態清理問題，避免記憶體洩漏
- 斷路器參數設置需要根據實際系統特性調整

## 常見重試策略比較

| 重試策略 | 適用場景 | 優點 | 缺點 |
|---------|---------|------|------|
| 簡單重試 | 基本的暫時性故障 | 易於實現和理解 | 不夠靈活 |
| 指數退避 | 網絡連接、服務調用 | 減輕目標系統負擔 | 等待時間可能過長 |
| 隨機退避 | 高併發系統 | 避免請求同步，減少衝突 | 行為不夠可預測 |
| 有狀態重試 | 需要跨請求保持重試狀態 | 支持複雜的重試邏輯 | 實現較為複雜 |
| 斷路器 | 分佈式系統 | 防止系統級聯故障 | 需要精心調校參數 |

## 參考資料

- [Spring Retry 官方文檔](https://docs.spring.io/spring-retry/docs/latest/reference/html/)
- [Spring Retry GitHub](https://github.com/spring-projects/spring-retry)
- [Spring Boot Retry 整合](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-retrying-operations)
- [Netflix Hystrix](https://github.com/Netflix/Hystrix) (一個類似的斷路器實現，現已停止維護)
- [Resilience4j](https://github.com/resilience4j/resilience4j) (現代彈性庫，可與Spring Retry結合使用)
