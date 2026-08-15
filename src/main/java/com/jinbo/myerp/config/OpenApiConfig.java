package com.jinbo.myerp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * 전역 보안 요구사항(security)은 일부러 지정하지 않는다. 여기서 지정하면 /api/auth/**처럼
 * 실제로 인증이 필요 없는 엔드포인트에도 자물쇠 아이콘이 붙어 문서가 부정확해지므로,
 * 인증이 필요한 컨트롤러에만 개별적으로 @SecurityRequirement("bearerAuth")를 붙인다.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "MyERP API",
                version = "v1",
                description = "소규모 건축자재 도소매업을 위한 간이 ERP 시스템 API 명세. "
                        + "인증(회원가입/로그인), 거래처, 카테고리/품목/규격/재고, 매입/매출 전표를 다룬다."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "로그인(POST /api/auth/login) 응답의 accessToken을 그대로 입력한다."
)
public class OpenApiConfig {
}
