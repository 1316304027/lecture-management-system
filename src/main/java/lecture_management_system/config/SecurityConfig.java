package lecture_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security設定
 *
 * ★重要★
 * このアプリは独自のセッション認証（LoginController）を使用するため
 * Spring Securityのフォームログイン機能は無効にしている。
 * 代わりに、ControllerでセッションからloginUserを取得して認証チェックを行う。
 *
 * BCryptPasswordEncoder を @Bean として登録する。
 * UserService でパスワードのハッシュ化と照合に使用する。
 */
@Configuration
public class SecurityConfig {

    /**
     * パスワードハッシュ化エンコーダー
     * 強度（strength）デフォルト10を使用する。
     * UserService.createUser() でパスワードをBCryptでハッシュ化してDBに保存する。
     * UserService.checkPassword() でログイン時のパスワード照合に使用する。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * セキュリティフィルターチェーン
     *
     * 独自セッション認証を使用するため、Spring Securityのアクセス制御は
     * 全て permitAll() にしている。
     * アクセス制御はController冒頭の session.getAttribute("loginUser") チェックで行う。
     *
     * CSRF は HTML フォームで問題が起きないよう無効にしている。
     * 本番環境では CSRF 保護を有効にすることを推奨する。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
