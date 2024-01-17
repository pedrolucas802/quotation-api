package org.br.mineradora.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.br.mineradora.client.CurrencyPriceClient;
import org.br.mineradora.dto.CurrencyPriceDTO;
import org.br.mineradora.dto.QuotationDTO;
import org.br.mineradora.entity.QuotationEntity;
import org.br.mineradora.message.KafkaEvents;
import org.br.mineradora.repository.QuotationRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class QuotationService {

    @Inject
    @RestClient
    CurrencyPriceClient currencyPriceClient;

    @Inject
    QuotationRepository quotationRepository;

    @Inject
    KafkaEvents kafkaEvents;

    public void getCurrencyPrice() {
        CurrencyPriceDTO currencyPriceinfo = currencyPriceClient.getPriceByPair("USD-BRL");

        if(updateCurrentInfoPrice(currencyPriceinfo)){
            kafkaEvents.sendNewKafkaEvent(QuotationDTO
                    .builder()
                    .currencyPrice(new BigDecimal(currencyPriceinfo.getUSDBRL().getBid()))
                    .date(new Date())
                    .build());

        }
    }

    private boolean updateCurrentInfoPrice(CurrencyPriceDTO currencyPriceinfo) {
            BigDecimal currentPrice = new BigDecimal(currencyPriceinfo.getUSDBRL().getBid());
            boolean updatePrice = false;

            List<QuotationEntity> quotationList = quotationRepository.findAll().list();

            if(quotationList.isEmpty()){
                saveQuotation(currencyPriceinfo);
                updatePrice=true;
            }else {
                QuotationEntity lastDollarPrice = quotationList.get(quotationList.size()-1);
                if(currentPrice.floatValue() > lastDollarPrice.getCurrencyPrice().floatValue()){
                    updatePrice=true;
                    saveQuotation(currencyPriceinfo);
                }

            }

            return updatePrice;
    }


    public void saveQuotation(CurrencyPriceDTO currencyInfo){
        QuotationEntity quotation = new QuotationEntity();

        quotation.setDate(new Date());
        quotation.setCurrencyPrice(new BigDecimal(currencyInfo.getUSDBRL().getBid()));
        quotation.setPctChange(currencyInfo.getUSDBRL().getPctChange());
        quotation.setPair("USD-BRL");

        quotationRepository.persist(quotation);
    }


}


