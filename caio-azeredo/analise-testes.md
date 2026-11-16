## 1. Exploração do módulo - Vacancies

| Campo | Obrigatório | Regra observada | Mensagem/comportamento |
|---|---|---|---|
| Vacancy Name | Sim | Máx. 50 caracteres | "Should not exceed 50 characters" |
| Vacancy Name | Sim | Exatamente 50 caracteres | Aceito normalmente |
| Vacancy Name | Sim | Vazio não é aceito | "Required" |
| Vacancy Name | Sim | Nome só com espaços não é aceito | "Required" |
| Vacancy Name | Sim | Duplicado é rejeitado, mesmo com espaço após o nome ou variação de case | "Already exists" |
| Job Title | Sim | Seleção em lista | "Required" se não selecionado um título |
| Hiring Manager | Sim | Só aceita funcionário selecionado no autocomplete | "Invalid" com texto livre |
| Hiring Manager | Sim | Espaço sozinho não retorna sugestões | "No Records Found" mesmo com nomes com espaço |
| Number of Positions | Não | Inteiro fora do intervalo 1 a 99 | "Should be a number between 1-99" |
| Number of Positions | Não | Inteiro de 1 a 99 (1, 99, 50) | Aceito |
| Active  | Não | Toggle | Aceito |
| Publish in RSS | Não | Toggle | Aceito |
| Attachment (só na edição) | Sim, para adicionar um anexo | Máx. 1MB | "Attachment Size Exceeded" |


**Versão analisada:** `56e23b3b09e7af29317a0943523b825843fff527`