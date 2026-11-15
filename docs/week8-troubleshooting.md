# 8주차 트러블슈팅

## No.1 컨트롤러 테스트가 컴파일되지 않음 (`void cannot be dereferenced`)

- **이슈**: `FavoriteControllerTest`가 `void cannot be dereferenced` 오류 12개로 컴파일에 실패했다.
- **문제**: 테스트 메서드 이름을 `status()`, `delete()`로 지었는데, 같은 클래스에서 정적 import한 `MockMvcResultMatchers.status()`, `MockMvcRequestBuilders.delete()`와 이름이 겹쳐 클래스 안의 void 메서드가 먼저 선택됐다.
- **해결**: `favoriteStatus()`, `deleteFavorite()`로 이름을 바꿨다. 이후 테스트 72개가 모두 통과했다.

## No.2 롤백이 정말 트랜잭션 덕분인지 확신이 없었음

- **이슈**: 409가 나면 평점도 안 남는 것을 확인했지만, 원래부터 안 남는 것일 수도 있다고 생각했다.
- **문제**: 성공한 결과만 비교하면 `@Transactional`의 효과인지 알 수 없다.
- **해결**: 같은 요청을 `@Transactional`을 뺀 코드에서 다시 보내 평점 행(ratingId 6)이 남는 것을 확인한 뒤 원복하고 실험 행을 지웠다.
