# Spring Retry 使用筆記

Spring Retry 是一個用於處理失敗操作的重試機制框架，特別適用於可能因暫時性故障而失敗的操作（如網路連接、資料庫操作等）。這個專案提供了
Spring Retry 的基本使用範例。

## 專案環境

- Java 21
- Spring Boot 3.4.4
- Gradle
- Spring Retry

## 專案結構

```
src/main/java/com/bill/springretry/
├── SpringRetryApplication.java (主應用程式入口點)
├── exception
│   └── TransientNetworkException.java (自定義的暫時性異常)
└── service
    └── ExternalService.java (示範 @Retryable 的服務)
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

本專案展示了如何處理網路請求可能出現的暫時性故障：

1. 我們創建了自定義的 `TransientNetworkException` 來模擬網路故障
2. `ExternalService` 使用 `@Retryable` 標記需要重試的方法
3. 方法內部有 70% 的概率會拋出異常，模擬請求失敗
4. 配置了最多重試 3 次，每次間隔 2 秒
5. 當所有重試都失敗時，執行 `@Recover` 標記的恢復方法

## 如何使用 Spring Retry

### 步驟 1: 添加依賴

在 Gradle 項目中添加依賴：

```gradle
implementation 'org.springframework.boot:spring-boot-starter-aop'
implementation 'org.springframework.retry:spring-retry'
```

### 步驟 2: 啟用重試機制

在主應用程序類上添加 `@EnableRetry` 註解：

```java

@EnableRetry
@SpringBootApplication
public class Application {
    // ...
}
```

### 步驟 3: 標記需要重試的方法

```java

@Service
public class MyService {

    @Retryable(
            retryFor = {SpecificException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void riskyOperation() {
        // 可能失敗的操作
    }

    @Recover
    public void recover(SpecificException e) {
        // 恢復邏輯
    }
}
```

### 步驟 4: 針對特定場景的配置

#### 指數退避重試

通過設置 `multiplier` 實現指數增長的重試間隔：

```java
@Retryable(backoff = @Backoff(delay = 1000, multiplier = 2))
```

這會使重試間隔變為: 1秒, 2秒, 4秒, 8秒...

#### 隨機退避重試

為了避免多個客戶端同時重試導致的"雪崩效應"：

```java
@Retryable(backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true))
```

#### 有條件重試

只有在特定條件下才進行重試：

```java
@Retryable(retryFor = {MyException.class}, maxAttempts = 4)
```

## 進階用法

### 1. 編程式重試 (不使用註解)

除了註解方式，Spring Retry 也支持編程式調用：

```java
RetryTemplate template = new RetryTemplate();

// 配置重試策略
SimpleRetryPolicy policy = new SimpleRetryPolicy();
policy.

setMaxAttempts(3);
template.

setRetryPolicy(policy);

// 配置退避策略
FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
backOffPolicy.

setBackOffPeriod(1500);
template.

setBackOffPolicy(backOffPolicy);

// 執行需要重試的操作
String result = template.execute(context -> {
    // 可能失敗的操作
    return callService();
});
```

### 2. 複雜的重試策略

#### 組合多種異常類型

```java
@Retryable(
        include = {SocketTimeoutException.class, ConnectException.class},
        exclude = {NullPointerException.class}
)
```

#### 使用回調監聽重試過程

```java
RetryTemplate template = new RetryTemplate();
template.

registerListener(new RetryListenerSupport() {
    @Override
    public <T, E extends Throwable > void onError (RetryContext context, RetryCallback < T, E > callback, Throwable
    throwable){
        // 記錄每次重試錯誤
    }
});
```

## 最佳實踐

1. **僅針對暫時性故障重試**：只有那些有可能成功的操作才值得重試
2. **設置合理的最大重試次數**：避免無限重試導致資源浪費
3. **使用指數退避或隨機退避**：防止所有客戶端同時重試
4. **設置超時機制**：配合重試使用，避免長時間阻塞
5. **記錄重試事件**：便於監控和診斷問題
6. **提供降級處理**：通過 `@Recover` 提供優雅降級

## 注意事項

- 重試機制會導致方法執行時間延長，需要在設計系統時考慮
- 針對非冪等操作重試需要特別小心（如增加訂單）
- 過多的重試可能會加重下游系統負擔
- 需要確保重試不會導致資源耗盡（數據庫連接、線程池等）

## 參考資料

- [Spring Retry 官方文檔](https://docs.spring.io/spring-retry/docs/latest/reference/html/)
- [Spring Retry GitHub](https://github.com/spring-projects/spring-retry)
