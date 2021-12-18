package foatto.shop.spring.controllers

import foatto.core.link.CustomRequest
import foatto.core.link.CustomResponse
import foatto.shop_core.app.CUSTOM_COMMAND_SAVE_DOC_PAYMENT
import foatto.shop_core.app.PARAM_DOC_PAYMENT_ID
import foatto.shop_core.app.PARAM_DOC_PAYMENT_SBERBANK
import foatto.shop_core.app.PARAM_DOC_PAYMENT_SERTIFICATE
import foatto.shop_core.app.PARAM_DOC_PAYMENT_TERMIMAL
import foatto.spring.CoreSpringApp
import foatto.sql.AdvancedConnection
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ShopCustomController {

    @PostMapping("/api/custom")
    fun custom(
        @RequestBody
        customRequest: CustomRequest
    ): CustomResponse {

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)
        val stm = conn.createStatement()

        when (customRequest.command) {
            CUSTOM_COMMAND_SAVE_DOC_PAYMENT -> {
                val docId = customRequest.hmData[PARAM_DOC_PAYMENT_ID]!!
                val payTerminal = customRequest.hmData[PARAM_DOC_PAYMENT_TERMIMAL]!!
                val paySberbank = customRequest.hmData[PARAM_DOC_PAYMENT_SBERBANK]!!
                val paySertificate = customRequest.hmData[PARAM_DOC_PAYMENT_SERTIFICATE]!!

                stm.executeUpdate(
                    """
                        UPDATE SHOP_doc SET
                        pay_terminal = $payTerminal ,      
                        pay_sberbank = $paySberbank ,      
                        pay_sertificat = $paySertificate                             
                        WHERE id = $docId                                                 
                    """
                )
            }
        }

        //--- зафиксировать любые изменения в базе/
        conn.commit()

        stm.close()
        conn.close()

        return CustomResponse()
    }

}